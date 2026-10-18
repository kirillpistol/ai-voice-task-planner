from datetime import datetime, timedelta, timezone

from sqlalchemy import create_engine
from sqlalchemy.orm import Session

from app.genesis_store import Base, GenesisTask
from app.genesis_memory import Comment, workspace


def test_preview_suggests_an_imported_task_and_respects_comment(monkeypatch):
    monkeypatch.delenv("GENESIS_PUBLIC_REPORT_DIR", raising=False)
    monkeypatch.delenv("GENESIS_SECTOR_SOURCES_JSON", raising=False)
    engine = create_engine("sqlite://")
    Base.metadata.create_all(engine)
    with Session(engine) as session:
        session.add(GenesisTask(
            user_id="alice", source_task_id="task-1",
            title="Подготовить отчёт", description="",
            completed=False,
            due_at=datetime.now(timezone.utc) - timedelta(days=1),
            imported_at=datetime.now(timezone.utc),
        ))
        session.commit()
        proposal = Comment(sector="FINANCE", kind="constraint",
                           comment="Сначала выполнить срочное", language="ru")
        result = workspace(session, "alice", "FINANCE", "ru", proposal, "preview")
        assert "Подсказка GENESIS:" in result["text"]
        assert "Подготовить отчёт" in result["text"]
        assert "Просрочено: 1" in result["text"]
        assert "Сначала выполнить срочное" in result["text"]
        assert session.query(GenesisTask).count() == 1
        other = workspace(session, "bob", "FINANCE", "ru", proposal, "preview")
        assert "Подготовить отчёт" not in other["text"]
    engine.dispose()


def test_preview_without_import_shows_next_step():
    engine = create_engine("sqlite://")
    Base.metadata.create_all(engine)
    with Session(engine) as session:
        proposal = Comment(sector="FINANCE", kind="goal",
                           comment="Спланировать неделю")
        result = workspace(session, "alice", "FINANCE", "ru", proposal, "preview")
        assert "Выгруженных незавершённых задач нет" in result["text"]
    engine.dispose()
