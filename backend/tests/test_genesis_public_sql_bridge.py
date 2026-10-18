import os
from datetime import datetime, timezone

from app.genesis_store import GenesisSourceObservation, make_session_factory
from app.genesis_memory import workspace
from app.genesis_public_check import check_and_store_demo


def test_public_observation_persists_and_populates_memory(monkeypatch, tmp_path):
    monkeypatch.setenv("GENESIS_PUBLIC_REPORT_DIR", str(tmp_path / "reports"))
    monkeypatch.setenv("GENESIS_SECTOR_SOURCES_JSON",
        '{"FINANCE":{"source_name":"Example public website","source_url":"https://www.example.com/",'
        '"resource_name":"Server HTTPS worker","resource_type":"web"}}')
    factory = make_session_factory("sqlite:///" + str(tmp_path / "genesis.sqlite"))
    now = datetime.now(timezone.utc).isoformat()
    result = {"sector": "FINANCE", "source_url": "https://www.example.com/",
              "resource": "server HTTPS check", "verified_at": now,
              "status": "verified_http_200", "bytes": 577,
              "sha256": "a" * 64, "content_analyzed": False,
              "external_app_executed": False}
    monkeypatch.setattr("app.genesis_public_check.check_public_demo", lambda sector: result)
    check_and_store_demo(factory, "FINANCE")
    with factory() as session:
        row = session.get(GenesisSourceObservation, "FINANCE")
        assert row is not None
        response = workspace(session, "test-user", "FINANCE", "ru")
        assert "HTTP 200 проверен" in response["source"]
        assert response["source_status"]["verification_completed"] is True
        assert "SHA-256" in response["connection_resource"]


def test_different_source_cannot_reuse_old_sql_result(monkeypatch, tmp_path):
    monkeypatch.setenv("GENESIS_PUBLIC_REPORT_DIR", str(tmp_path / "reports"))
    factory = make_session_factory("sqlite:///" + str(tmp_path / "genesis.sqlite"))
    result = {"sector": "FINANCE", "source_url": "https://www.example.com/",
              "resource": "server HTTPS check",
              "verified_at": datetime.now(timezone.utc).isoformat(),
              "status": "verified_http_200", "sha256": "b" * 64}
    monkeypatch.setattr("app.genesis_public_check.check_public_demo", lambda sector: result)
    check_and_store_demo(factory, "FINANCE")
    monkeypatch.setenv("GENESIS_SECTOR_SOURCES_JSON",
        '{"FINANCE":{"source_name":"Different source","source_url":"https://example.org/",'
        '"resource_name":"Different worker","resource_type":"web"}}')
    with factory() as session:
        response = workspace(session, "test-user", "FINANCE", "ru")
        assert "HTTP 200 проверен" not in response["source"]
        assert response["source_status"]["verification_completed"] is False
