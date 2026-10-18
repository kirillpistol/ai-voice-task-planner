from datetime import datetime, timezone
from sqlalchemy import create_engine
from sqlalchemy.orm import Session

from app.genesis_ai import _usage_detail, _period
from app.genesis_store import Base, GenesisAiUsageEvent


def test_details_are_user_scoped_and_count_completed_only():
    engine = create_engine("sqlite+pysqlite:///:memory:")
    Base.metadata.create_all(engine)
    now = datetime.now(timezone.utc)
    with Session(engine) as session:
        session.add_all([
            GenesisAiUsageEvent(id="done-a", user_id="a", period=_period(),
                status="completed", model="fixture", input_tokens=20,
                output_tokens=5, created_at=now),
            GenesisAiUsageEvent(id="failed-a", user_id="a", period=_period(),
                status="failed", model=None, input_tokens=None,
                output_tokens=None, created_at=now),
            GenesisAiUsageEvent(id="done-b", user_id="b", period=_period(),
                status="completed", model="fixture", input_tokens=999,
                output_tokens=999, created_at=now),
        ])
        session.commit()
        details = _usage_detail("a", session)
        assert details["plan"] == "free"
        assert details["limit"] == 100
        assert details["input_tokens"] == 20
        assert details["output_tokens"] == 5
        assert details["completed_requests"] == 1
        assert details["resets_at"].endswith("00:00:00+00:00")
        assert _usage_detail("c", session)["completed_requests"] == 0
