"""Quarterly retention for GENESIS analytical task copies only.

The scheduler is deliberately independent of Firestore calendar tasks.
Only copies imported BEFORE the start of the current UTC quarter are deleted.
Run periodically (including on startup); the condition is idempotent.
"""
from datetime import datetime, timezone
from sqlalchemy import delete
from sqlalchemy.orm import Session
from app.genesis_store import GenesisTask


def quarter_start(at: datetime) -> datetime:
    if at.tzinfo is None:
        raise ValueError("Quarterly retention requires an aware datetime")
    at = at.astimezone(timezone.utc)
    month = ((at.month - 1) // 3) * 3 + 1
    return datetime(at.year, month, 1, tzinfo=timezone.utc)


def prune_old_analytical_tasks(session: Session, at: datetime | None = None) -> int:
    """Keep current-quarter analytical task copies; never touch recommendations."""
    now = at or datetime.now(timezone.utc)
    result = session.execute(
        delete(GenesisTask).where(GenesisTask.imported_at < quarter_start(now))
    )
    session.commit()
    return int(result.rowcount or 0)
