"""End-to-end API contract: observed Level-3 status -> authenticated GENESIS cards."""
import json
from datetime import datetime, timezone

from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.main import app
from app.genesis_store import Base, GenesisSourceObservation, require_sql_session
from app.v2 import current_uid


def test_authenticated_memory_endpoint_shows_saved_level3_result(monkeypatch, tmp_path):
    monkeypatch.setenv("GENESIS_PUBLIC_REPORT_DIR", str(tmp_path / "unused-reports"))
    monkeypatch.setenv("GENESIS_SECTOR_SOURCES_JSON", json.dumps({
        "FINANCE": {
            "source_name": "Public demo",
            "source_url": "https://www.example.com/",
            "resource_name": "Server HTTPS worker",
            "resource_type": "web",
        }
    }))
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False},
                           poolclass=StaticPool)
    Base.metadata.create_all(engine)
    factory = sessionmaker(bind=engine, expire_on_commit=False)
    with factory() as session:
        observed = {
            "source_url": "https://www.example.com/",
            "verified_at": "2026-10-16T19:22:57+00:00",
            "status": "verified_http_200",
            "sha256": "a" * 64,
        }
        session.add(GenesisSourceObservation(
            sector="FINANCE", source_url=observed["source_url"],
            resource="server HTTPS check", status="verified_http_200",
            checked_at=datetime.fromisoformat(observed["verified_at"]),
            digest=observed["sha256"], result_json=json.dumps(observed),
        ))
        session.commit()

    def sql_session():
        with factory() as session:
            yield session

    app.dependency_overrides[current_uid] = lambda: "test-user"
    app.dependency_overrides[require_sql_session] = sql_session
    try:
        with TestClient(app) as client:
            response = client.get("/api/v2/genesis/memory/FINANCE?language=ru")
        assert response.status_code == 200, response.text
        data = response.json()
        assert "HTTP 200 проверен" in data["source"]
        assert "SHA-256" in data["connection_resource"]
        assert data["source_status"]["verification_completed"] is True
        assert "Level 3:" in data["text"]
        assert data["execution_allowed"] is False
    finally:
        app.dependency_overrides.clear()
        engine.dispose()
