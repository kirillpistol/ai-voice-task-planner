"""SQL and API isolation tests without connecting to a real user's Firestore."""
from datetime import datetime, timezone

from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.genesis_store import Base, GenesisRecommendation, require_sql_session


def test_import_delete_and_reimport_with_explicit_consent(monkeypatch):
    # Use the real FastAPI route and override only external credentials/sources.
    from app.main import app
    from app.v2 import current_uid, database

    class CalendarTask:
        id = "calendar-id"
        def to_dict(self):
            return {"title": "From calendar", "description": "",
                    "completed": False, "due_at": "2026-10-11T10:00:00Z"}

    class Calendar:
        def collection(self, name):
            return self

        def document(self, name):
            return self

        def stream(self):
            return [CalendarTask()]

    engine = create_engine("sqlite+pysqlite:///:memory:",
                           connect_args={"check_same_thread": False}, poolclass=StaticPool)
    Base.metadata.create_all(engine)
    factory = sessionmaker(bind=engine)
    app.dependency_overrides[current_uid] = lambda: "person-a"
    app.dependency_overrides[database] = lambda: Calendar()

    def sql_session():
        with factory() as s:
            yield s

    app.dependency_overrides[require_sql_session] = sql_session
    monkeypatch.setenv("GENESIS_PRIVACY_URL", "https://example.org/published-policy")
    monkeypatch.setenv("GENESIS_POLICY_VERSION", "v1")
    monkeypatch.setenv("GENESIS_DATABASE_URL", "sqlite+pysqlite:///:memory:")

    with TestClient(app) as client:
        path = "/api/v2/genesis"
        payload = {"consent_to_personal_analysis": True, "policy_version": "v1"}
        assert client.get(path + "/config").json()["import_available"] is True
        assert client.post(path + "/tasks/import", json={**payload, "consent_to_personal_analysis": False}).status_code == 400
        assert client.post(path + "/tasks/import", json={**payload, "policy_version": "wrong"}).status_code == 400
        assert client.post(path + "/tasks/import", json=payload).json() == {"imported": 1, "total": 1}
        assert client.post(path + "/tasks/import", json=payload).json()["imported"] == 0

        with factory() as s:
            s.add(GenesisRecommendation(
                id="rec-1", user_id="person-a", summary="Retained insight",
                generated_at=datetime.now(timezone.utc)
            ))
            s.commit()

        assert client.delete(path + "/tasks").status_code == 200
        assert client.get(path + "/tasks").json() == []
        assert len(client.get(path + "/recommendations").json()) == 1

        assert client.post(path + "/tasks/import", json=payload).json()["imported"] == 1
        # Other user cannot view or delete these copies/recommendations.
        app.dependency_overrides[current_uid] = lambda: "person-b"
        assert client.get(path + "/tasks").json() == []
        assert client.get(path + "/recommendations").json() == []
        assert client.delete(path + "/tasks/calendar-id").status_code == 404
        app.dependency_overrides[current_uid] = lambda: "person-a"
        assert client.delete(path + "/tasks/calendar-id").status_code == 204
        assert client.get(path + "/tasks").json() == []

    app.dependency_overrides.clear()
