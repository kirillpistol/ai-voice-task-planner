"""Server-owned GENESIS plan resolution. No self-service upgrades."""
from datetime import datetime, timezone
from sqlalchemy.orm import Session
from app.genesis_store import GenesisSubscription

PLAN_LIMITS = {"free": 100, "plus": 1000}


def effective_plan(uid: str, session: Session, now: datetime | None = None) -> str:
    now = now or datetime.now(timezone.utc)
    subscription = session.get(GenesisSubscription, uid)
    if subscription is None or subscription.plan_id not in PLAN_LIMITS:
        return "free"
    if subscription.plan_id == "free":
        return "free"
    expires = subscription.valid_until
    if expires is None:
        return "free"
    if expires.tzinfo is None:
        expires = expires.replace(tzinfo=timezone.utc)
    return subscription.plan_id if expires > now else "free"


def effective_limit(uid: str, session: Session, now: datetime | None = None) -> int:
    return PLAN_LIMITS[effective_plan(uid, session, now)]
