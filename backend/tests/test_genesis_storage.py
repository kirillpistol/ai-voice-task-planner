from datetime import datetime, timezone

from sqlalchemy import create_engine, select
from sqlalchemy.orm import sessionmaker

from app.genesis_store import Base, GenesisTask, GenesisRecommendation, configured_database_url
from app.quarterly_cleanup import purge_previous_quarters


def test_quarterly_reset_preserves_recommendations_and_new_quarter():
    engine = create_engine("sqlite+pysqlite:///:memory:")
    Base.metadata.create_all(engine)
    factory = sessionmaker(engine)
    with factory() as s:
        s.add(GenesisTask(user_id="a", source_task_id="old", title="Old",
                          description="", completed=False,
                          imported_at=datetime(2026, 3, 20, tzinfo=timezone.utc)))
        s.add(GenesisTask(user_id="a", source_task_id="new", title="New",
                          description="", completed=False,
                          imported_at=datetime(2026, 4, 2, tzinfo=timezone.utc)))
        s.add(GenesisRecommendation(id="r1", user_id="a",
                                    summary="Keep the insight",
                                    generated_at=datetime(2026, 3, 10, tzinfo=timezone.utc)))
        s.commit()
    assert purge_previous_quarters(datetime(2026, 4, 5, tzinfo=timezone.utc), factory) == 1
    assert purge_previous_quarters(datetime(2026, 4, 5, tzinfo=timezone.utc), factory) == 0
    with factory() as s:
        assert [t.source_task_id for t in s.scalars(select(GenesisTask)).all()] == ["new"]
        assert [r.summary for r in s.scalars(select(GenesisRecommendation)).all()] == ["Keep the insight"]


def test_sql_password_is_read_from_server_mounted_file(tmp_path, monkeypatch):
    url_file = tmp_path / "genesis-db-url"
    url_file.write_text("postgresql+psycopg://genesis_app:secret@genesis-postgres/genesis\n")
    monkeypatch.setenv("GENESIS_DATABASE_URL", "invalid-fallback")
    monkeypatch.setenv("GENESIS_DATABASE_URL_FILE", str(url_file))
    assert configured_database_url() == (
        "postgresql+psycopg://genesis_app:secret@genesis-postgres/genesis"
    )
