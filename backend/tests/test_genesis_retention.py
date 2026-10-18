from datetime import datetime, timezone
from sqlalchemy import create_engine, select
from sqlalchemy.orm import Session

from app.genesis_retention import quarter_start, prune_old_analytical_tasks
from app.genesis_store import Base, GenesisTask, GenesisRecommendation


def test_quarter_starts_in_utc():
    assert quarter_start(datetime(2026, 10, 18, tzinfo=timezone.utc)) == datetime(2026, 10, 1, tzinfo=timezone.utc)
    assert quarter_start(datetime(2027, 1, 1, tzinfo=timezone.utc)) == datetime(2027, 1, 1, tzinfo=timezone.utc)


def test_only_prior_quarter_task_copies_removed():
    engine = create_engine("sqlite+pysqlite:///:memory:")
    Base.metadata.create_all(engine)
    now = datetime(2026, 10, 18, tzinfo=timezone.utc)
    with Session(engine) as session:
        session.add_all([
            GenesisTask(user_id="a", source_task_id="old", title="Old", imported_at=datetime(2026, 9, 30, tzinfo=timezone.utc)),
            GenesisTask(user_id="a", source_task_id="new", title="New", imported_at=datetime(2026, 10, 2, tzinfo=timezone.utc)),
            GenesisRecommendation(id="fixed", user_id="a", summary="Persistent result", generated_at=now),
        ])
        session.commit()
        assert prune_old_analytical_tasks(session, now) == 1
        assert prune_old_analytical_tasks(session, now) == 0
        assert [task.source_task_id for task in session.scalars(select(GenesisTask)).all()] == ["new"]
        assert session.get(GenesisRecommendation, "fixed").summary == "Persistent result"
