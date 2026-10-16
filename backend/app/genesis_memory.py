"""User-confirmed text memory. No model training, code execution or core access."""
import hashlib
import os
from datetime import datetime, timezone
from typing import Literal

from fastapi import APIRouter, Depends, HTTPException, Request
from pydantic import BaseModel, ConfigDict, Field, field_validator
from sqlalchemy import DateTime, String, Text, select
from sqlalchemy.orm import Mapped, Session, mapped_column

from app.genesis_store import Base, require_sql_session
from app.genesis_security_profiles import selected_profile, profile_text
from app.genesis_source_registry import sector_source_status
from app.v2 import current_uid

router = APIRouter(prefix="/api/v2/genesis/memory", tags=["genesis-memory"])
Sector = Literal["FINANCE", "MEDICINE", "MANUFACTURING", "MILITARY", "GOVERNMENT", "LOGISTICS"]
Kind = Literal["goal", "constraint", "preference", "correction"]
MAX_RULES = 50


class MemoryConsent(Base):
    __tablename__ = "genesis_memory_consents"
    user_id: Mapped[str] = mapped_column(String(128), primary_key=True)
    policy_version: Mapped[str] = mapped_column(String(80), nullable=False)
    accepted_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)


class MemoryRule(Base):
    __tablename__ = "genesis_memory_rules"
    user_id: Mapped[str] = mapped_column(String(128), primary_key=True)
    sector: Mapped[str] = mapped_column(String(24), primary_key=True)
    id: Mapped[str] = mapped_column(String(64), primary_key=True)
    kind: Mapped[str] = mapped_column(String(16), nullable=False)
    text: Mapped[str] = mapped_column(Text, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)


class Comment(BaseModel):
    model_config = ConfigDict(extra="forbid")
    sector: Sector
    kind: Kind
    comment: str = Field(min_length=1, max_length=2000)
    language: Literal["ru", "en"] = "ru"

    @field_validator("comment")
    @classmethod
    def nonempty(cls, value):
        value = value.strip()
        if not value or any(ord(char) < 32 and char not in "\n\r\t" for char in value):
            raise ValueError("Comment must contain text without control characters")
        return value


class Confirmation(Comment):
    policy_version: str = Field(min_length=1, max_length=80)
    consent_to_store: bool = False


def memory_policy():
    url = os.getenv("GENESIS_MEMORY_PRIVACY_URL", "").strip()
    version = os.getenv("GENESIS_MEMORY_POLICY_VERSION", "").strip()
    return (url if url.startswith("https://") else None), version


def require_policy():
    url, version = memory_policy()
    if not url or not version:
        raise HTTPException(503, "Memory policy is not published")
    return version


def rules_for(session, uid, sector):
    return session.scalars(select(MemoryRule).where(
        MemoryRule.user_id == uid, MemoryRule.sector == sector
    ).order_by(MemoryRule.created_at, MemoryRule.id)).all()


def workspace(session, uid, sector, language, proposal=None, status="context"):
    rows = rules_for(session, uid, sector)
    rules = [{"id": row.id, "kind": row.kind, "text": row.text} for row in rows]
    ru = language == "ru"
    names = {"goal": "Цель", "constraint": "Ограничение", "preference": "Предпочтение", "correction": "Исправление"}
    lines = ["Подтверждённые правила раздела:" if ru else "Confirmed sector rules:"]
    for i, row in enumerate(rows, 1):
        lines.append(f"{i}. [{names[row.kind] if ru else row.kind}] {row.text}")
    if not rows:
        lines.append("Пока нет подтверждённых правил." if ru else "No confirmed rules yet.")
    if proposal is not None:
        lines.extend(["", "Предложение — ещё не сохранено:" if ru else "Proposal — not saved yet:",
                      f"[{names[proposal.kind] if ru else proposal.kind}] {proposal.comment}"])
    if status == "confirmed":
        lines.extend(["", "Правило сохранено." if ru else "Rule saved."])
    lines.extend(["", "Это подготовка контекста. Алгоритм роли и обучение модели не подключены."
                  if ru else "This prepares context. Sector algorithms and model training are not connected."])
    _, version = memory_policy()
    connection = sector_source_status(sector, language)
    consent = session.get(MemoryConsent, uid)
    return {
        "status": status, "text": "\n".join(lines), "rules": rules,
        "consent_accepted": bool(version and consent and consent.policy_version == version),
        "source": connection["source"],
        "connection_resource": connection["connection_resource"],
        "source_status": {key: connection[key] for key in ("source_configured", "resource_configured", "verification_completed", "worker_executed")},
        "security_profile": selected_profile(sector),
        "security_system": profile_text(sector, language),
        "execution_allowed": False,
    }


@router.get("/config")
def config(request: Request):
    url, version = memory_policy()
    return {"policy_url": url, "policy_version": version,
            "available": bool(url and version and getattr(request.app.state, "genesis_sql_factory", None)),
            "execution_allowed": False}


@router.get("/{sector}")
def read_memory(sector: Sector, language: Literal["ru", "en"] = "ru",
                uid: str = Depends(current_uid), session: Session = Depends(require_sql_session)):
    return workspace(session, uid, sector, language)


@router.post("/preview")
def preview(request: Comment, uid: str = Depends(current_uid), session: Session = Depends(require_sql_session)):
    require_policy()
    return workspace(session, uid, request.sector, request.language, request, "preview")


@router.post("/confirm")
def confirm(request: Confirmation, uid: str = Depends(current_uid), session: Session = Depends(require_sql_session)):
    version = require_policy()
    # Serialize writes for this account in PostgreSQL, including the first consent.
    if session.get_bind().dialect.name == "postgresql":
        from sqlalchemy import text
        session.execute(text("SELECT pg_advisory_xact_lock(:key)"),
                        {"key": int.from_bytes(hashlib.sha256(uid.encode()).digest()[:8], "big", signed=True)})
    consent = session.get(MemoryConsent, uid)
    accepted = consent is not None and consent.policy_version == version
    if request.policy_version != version or (not accepted and request.consent_to_store is not True):
        raise HTTPException(400, "Current memory consent is required")
    # Whitespace-equivalent retries cannot add another rule.
    canonical = " ".join(request.comment.split())
    rule_id = hashlib.sha256((request.kind + "\0" + canonical).encode()).hexdigest()
    key = (uid, request.sector, rule_id)
    if session.get(MemoryRule, key) is None:
        if len(rules_for(session, uid, request.sector)) >= MAX_RULES:
            raise HTTPException(409, "Sector memory is full")
        now = datetime.now(timezone.utc)
        session.add(MemoryRule(user_id=uid, sector=request.sector, id=rule_id,
                               kind=request.kind, text=canonical, created_at=now))
        if not accepted:
            if consent is None:
                session.add(MemoryConsent(user_id=uid, policy_version=version, accepted_at=now))
            else:
                consent.policy_version, consent.accepted_at = version, now
    elif not accepted:
        now = datetime.now(timezone.utc)
        if consent is None:
            session.add(MemoryConsent(user_id=uid, policy_version=version, accepted_at=now))
        else:
            consent.policy_version, consent.accepted_at = version, now
    session.commit()
    return workspace(session, uid, request.sector, request.language, status="confirmed")


@router.delete("/{sector}/{rule_id}", status_code=204)
def forget(sector: Sector, rule_id: str, uid: str = Depends(current_uid), session: Session = Depends(require_sql_session)):
    row = session.get(MemoryRule, (uid, sector, rule_id))
    if row is None:
        raise HTTPException(404, "Rule not found")
    session.delete(row)
    session.commit()
