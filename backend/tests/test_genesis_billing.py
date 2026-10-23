"""GENESIS billing API helpers: scope, quota and truthful pricing."""
from datetime import datetime, timezone
from sqlalchemy import create_engine
from sqlalchemy.orm import Session

from app.genesis_ai import _billing_events, _billing_summary, _period
from app.genesis_store import Base, GenesisAiQuota, GenesisAiUsageEvent


def test_billing_summary_reports_remaining_and_warning_without_fake_cost():
    engine = create_engine("sqlite+pysqlite:///:memory:")
    Base.metadata.create_all(engine)
    with Session(engine) as session:
        session.add(GenesisAiQuota(user_id="alice", period=_period(), used=80))
        session.commit()
        result = _billing_summary("alice", session)
        assert result["limit"] == 100
        assert result["remaining"] == 20
        assert result["warning_level"] == "warning"
        assert result["usage_percent"] == 80.0
        assert result["estimated_cost"] is None
        assert result["payments_enabled"] is False
        assert _billing_summary("bob", session)["remaining"] == 100


def test_billing_event_history_never_leaks_other_accounts():
    engine = create_engine("sqlite+pysqlite:///:memory:")
    Base.metadata.create_all(engine)
    now = datetime.now(timezone.utc)
    with Session(engine) as session:
        session.add_all([
            GenesisAiUsageEvent(id="alice-1", user_id="alice", period=_period(),
                status="completed", model="fixture", input_tokens=20,
                output_tokens=5, created_at=now),
            GenesisAiUsageEvent(id="bob-1", user_id="bob", period=_period(),
                status="failed", model=None, input_tokens=None,
                output_tokens=None, created_at=now),
        ])
        session.commit()
        events = _billing_events("alice", session)
        assert len(events) == 1
        assert events[0]["id"] == "alice-1"
        assert events[0]["quota_units"] == 1
        assert "user_id" not in events[0]
