"""Separate SQL workspace for GENESIS; calendar tasks stay in Firestore."""
import os
from datetime import datetime, timezone
from typing import Optional

from fastapi import HTTPException
from sqlalchemy import Boolean, DateTime, Index, Integer, String, Text, create_engine
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, sessionmaker


class Base(DeclarativeBase):
    pass


class GenesisTask(Base):
    __tablename__ = "genesis_tasks"
    user_id: Mapped[str] = mapped_column(String(128), primary_key=True)
    source_task_id: Mapped[str] = mapped_column(String(128), primary_key=True)
    title: Mapped[str] = mapped_column(String(200), nullable=False)
    description: Mapped[str] = mapped_column(Text, default="", nullable=False)
    due_at: Mapped[Optional[datetime]] = mapped_column(DateTime(timezone=True))
    completed: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    imported_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False,
        default=lambda: datetime.now(timezone.utc)
    )
    __table_args__ = (Index("ix_genesis_tasks_user_imported", "user_id", "imported_at"),)


class GenesisConsent(Base):
    __tablename__ = "genesis_consents"
    user_id: Mapped[str] = mapped_column(String(128), primary_key=True)
    policy_version: Mapped[str] = mapped_column(String(80), nullable=False)
    accepted_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)


class GenesisSourceObservation(Base):
    """Latest sector source observation, produced by a server-only worker."""
    __tablename__ = "genesis_source_observations"
    sector: Mapped[str] = mapped_column(String(24), primary_key=True)
    source_url: Mapped[str] = mapped_column(String(2048), nullable=False)
    resource: Mapped[str] = mapped_column(String(120), nullable=False)
    status: Mapped[str] = mapped_column(String(40), nullable=False)
    checked_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    digest: Mapped[str] = mapped_column(String(64), nullable=False)
    result_json: Mapped[str] = mapped_column(Text, nullable=False)


class GenesisEnterpriseInquiry(Base):
    """Customer-requested custom GENESIS deployment; no payment credentials."""
    __tablename__ = "genesis_enterprise_inquiries"
    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    user_id: Mapped[str] = mapped_column(String(128), nullable=False, index=True)
    comment: Mapped[str] = mapped_column(Text, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)


class GenesisAiUsageEvent(Base):
    """Server-only accounting record; separate from wallet/payment balances."""
    __tablename__ = "genesis_ai_usage_events"
    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    user_id: Mapped[str] = mapped_column(String(128), nullable=False, index=True)
    period: Mapped[str] = mapped_column(String(7), nullable=False)
    model: Mapped[Optional[str]] = mapped_column(String(120))
    input_tokens: Mapped[Optional[int]] = mapped_column(Integer)
    output_tokens: Mapped[Optional[int]] = mapped_column(Integer)
    status: Mapped[str] = mapped_column(String(24), nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)


class GenesisAiQuota(Base):
    """Authoritative per-user UTC-month request counter; no client writes."""
    __tablename__ = "genesis_ai_quota"
    user_id: Mapped[str] = mapped_column(String(128), primary_key=True)
    period: Mapped[str] = mapped_column(String(7), primary_key=True)
    used: Mapped[int] = mapped_column(Integer, nullable=False, default=0)


class GenesisRecommendation(Base):
    __tablename__ = "genesis_recommendations"
    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    user_id: Mapped[str] = mapped_column(String(128), index=True, nullable=False)
    summary: Mapped[str] = mapped_column(Text, nullable=False)
    generated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)


def configured_policy_url() -> Optional[str]:
    url = os.environ.get("GENESIS_PRIVACY_URL", "").strip()
    return url if url.startswith("https://") else None


def policy_version() -> str:
    return os.environ.get("GENESIS_POLICY_VERSION", "").strip()


def configured_database_url() -> str:
    """Load SQL credentials from a container-mounted secret or environment.

    Mounting a file avoids exposing passwords through docker inspect .Config.Env.
    """
    secret_path = os.environ.get("GENESIS_DATABASE_URL_FILE", "").strip()
    if secret_path:
        with open(secret_path, "r", encoding="utf-8") as secret_file:
            return secret_file.read().strip()
    return os.environ.get("GENESIS_DATABASE_URL", "").strip()


def make_session_factory(database_url: str):
    engine = create_engine(database_url, pool_pre_ping=True)
    Base.metadata.create_all(engine)  # Initial bootstrap; use migrations for upgrades.
    return sessionmaker(bind=engine, expire_on_commit=False)


def require_sql_session():
    from app.main import app
    factory = getattr(app.state, "genesis_sql_factory", None)
    if factory is None:
        raise HTTPException(status_code=503, detail="GENESIS SQL storage is not configured")
    with factory() as session:
        yield session
