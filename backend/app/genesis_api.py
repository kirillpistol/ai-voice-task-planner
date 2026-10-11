"""Opt-in SQL workspace. Firestore calendar is the only source of task contents."""
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, Request
from pydantic import BaseModel, Field
from sqlalchemy import delete, select
from sqlalchemy.orm import Session

from app.genesis_store import (
    GenesisConsent, GenesisRecommendation, GenesisTask, configured_policy_url,
    policy_version, require_sql_session,
)
from app.v2 import current_uid, database, task_collection

router = APIRouter(prefix="/api/v2/genesis", tags=["genesis"])
MAX_SOURCE_TASKS = 10000


class ImportRequest(BaseModel):
    consent_to_personal_analysis: bool
    policy_version: str = Field(min_length=1, max_length=80)


class ImportedTask(BaseModel):
    source_task_id: str
    title: str
    description: str
    due_at: datetime | None
    completed: bool


@router.get("/config")
def get_config(request: Request):
    url = configured_policy_url()
    version = policy_version()
    return {
        "policy_url": url,
        "policy_version": version,
        "import_available": bool(
            url and version and getattr(request.app.state, "genesis_sql_factory", None)
        ),
        "purpose": "personal_analysis_only",
    }


@router.get("/tasks", response_model=list[ImportedTask])
def list_imported(
    uid: str = Depends(current_uid),
    session: Session = Depends(require_sql_session),
):
    rows = session.scalars(
        select(GenesisTask).where(GenesisTask.user_id == uid)
        .order_by(GenesisTask.imported_at, GenesisTask.source_task_id)
    ).all()
    return [ImportedTask(
        source_task_id=task.source_task_id, title=task.title,
        description=task.description, due_at=task.due_at, completed=task.completed
    ) for task in rows]


@router.post("/tasks/import")
def import_calendar_tasks(
    request: ImportRequest,
    uid: str = Depends(current_uid),
    source=Depends(database),
    session: Session = Depends(require_sql_session),
):
    if not configured_policy_url() or not policy_version():
        raise HTTPException(status_code=503, detail="Published privacy policy is required")
    if request.consent_to_personal_analysis is not True or request.policy_version != policy_version():
        raise HTTPException(status_code=400, detail="Current personal-analysis consent is required")

    # Source records are loaded by the backend from this Firebase user's Firestore
    # namespace. No client-supplied texts, task IDs or other user IDs are accepted.
    docs = []
    for doc in task_collection(uid, source).stream():
        docs.append(doc)
        if len(docs) > MAX_SOURCE_TASKS:
            raise HTTPException(status_code=413, detail="Calendar exceeds import limit")

    now = datetime.now(timezone.utc)
    existing = set(session.scalars(
        select(GenesisTask.source_task_id).where(GenesisTask.user_id == uid)
    ).all())
    added = 0
    for doc in docs:
        if doc.id in existing:
            continue
        data = doc.to_dict() or {}
        title = data.get("title")
        if not isinstance(title, str) or not title.strip():
            continue
        raw_due_at = data.get("due_at")
        try:
            due_at = (
                datetime.fromisoformat(raw_due_at.replace("Z", "+00:00"))
                if isinstance(raw_due_at, str) else raw_due_at
            )
        except ValueError:
            due_at = None
        if isinstance(due_at, datetime):
            if due_at.tzinfo is None:
                due_at = due_at.replace(tzinfo=timezone.utc)
            due_at = due_at.astimezone(timezone.utc)
        else:
            due_at = None

        session.add(GenesisTask(
            user_id=uid, source_task_id=doc.id, title=title[:200],
            description=str(data.get("description") or "")[:5000],
            due_at=due_at, completed=bool(data.get("completed", False)),
            imported_at=now,
        ))
        existing.add(doc.id)
        added += 1

    consent = session.get(GenesisConsent, uid)
    if consent is None:
        session.add(GenesisConsent(
            user_id=uid, policy_version=policy_version(), accepted_at=now
        ))
    else:
        consent.policy_version = policy_version()
        consent.accepted_at = now
    session.commit()
    return {"imported": added, "total": len(existing)}


@router.delete("/tasks")
def clear_personal_model(
    uid: str = Depends(current_uid),
    session: Session = Depends(require_sql_session),
):
    count = session.scalar(select(GenesisTask.source_task_id)
                           .where(GenesisTask.user_id == uid).limit(1))
    session.execute(delete(GenesisTask).where(GenesisTask.user_id == uid))
    session.commit()
    return {
        "cleared": True,
        "had_tasks": count is not None,
        "recommendations_preserved": True,
    }


@router.delete("/tasks/{source_task_id}", status_code=204)
def delete_imported_task(
    source_task_id: str,
    uid: str = Depends(current_uid),
    session: Session = Depends(require_sql_session),
):
    row = session.get(GenesisTask, (uid, source_task_id))
    if row is None:
        raise HTTPException(status_code=404, detail="Imported task not found")
    session.delete(row)
    session.commit()
    return None


@router.get("/recommendations")
def get_recommendations(
    uid: str = Depends(current_uid),
    session: Session = Depends(require_sql_session),
):
    rows = session.scalars(
        select(GenesisRecommendation).where(GenesisRecommendation.user_id == uid)
        .order_by(GenesisRecommendation.generated_at.desc()).limit(100)
    ).all()
    return [{
        "id": row.id,
        "summary": row.summary,
        "generated_at": row.generated_at
    } for row in rows]
