import json
from app import genesis_level3_runner


def test_one_shot_runner_calls_persistence(monkeypatch):
    monkeypatch.setattr(genesis_level3_runner, "configured_database_url", lambda: "sqlite:///test.db")
    monkeypatch.setattr(genesis_level3_runner, "make_session_factory", lambda url: "factory")
    monkeypatch.setattr(genesis_level3_runner, "check_and_store_demo",
                        lambda factory, sector: {
                            "status": "verified_http_200", "sector": sector,
                            "verified_at": "2026-10-16T00:00:00+00:00",
                            "source_url": "https://www.example.com/",
                            "sha256": "a" * 64,
                        })
    result = genesis_level3_runner.execute_once()
    assert result["status"] == "verified_http_200"
    assert result["content_analyzed"] is False
    assert result["external_app_executed"] is False


def test_runner_requires_sql(monkeypatch):
    monkeypatch.setattr(genesis_level3_runner, "configured_database_url", lambda: "")
    assert genesis_level3_runner.main(["--once"]) == 1
