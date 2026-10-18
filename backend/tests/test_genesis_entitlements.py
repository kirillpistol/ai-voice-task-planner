from datetime import datetime, timedelta, timezone
from sqlalchemy import create_engine
from sqlalchemy.orm import Session
from app.genesis_store import Base, GenesisSubscription
from app.genesis_entitlements import effective_plan, effective_limit


def test_default_expired_and_server_granted_plus():
    engine = create_engine("sqlite+pysqlite:///:memory:")
    Base.metadata.create_all(engine)
    now = datetime.now(timezone.utc)
    with Session(engine) as session:
        assert effective_plan("alice", session, now) == "free"
        assert effective_limit("alice", session, now) == 100
        session.add(GenesisSubscription(
            user_id="alice", plan_id="plus", valid_until=now + timedelta(days=30),
            granted_at=now, source="server_test"))
        session.commit()
        assert effective_plan("alice", session, now) == "plus"
        assert effective_limit("alice", session, now) == 1000
        assert effective_plan("bob", session, now) == "free"
        assert effective_plan("alice", session, now + timedelta(days=31)) == "free"


def test_indefinite_plus_entitlement_is_not_accepted():
    engine = create_engine("sqlite+pysqlite:///:memory:")
    Base.metadata.create_all(engine)
    now = datetime.now(timezone.utc)
    with Session(engine) as session:
        session.add(GenesisSubscription(
            user_id="alice", plan_id="plus", valid_until=None,
            granted_at=now, source="test"))
        session.commit()
        assert effective_plan("alice", session, now) == "free"
