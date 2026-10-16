import json

from app.genesis_source_registry import sector_source_status


def test_unconfigured_source_is_explicit(monkeypatch):
    monkeypatch.delenv("GENESIS_SECTOR_SOURCES_JSON", raising=False)
    result = sector_source_status("FINANCE")
    assert result["source_configured"] is False
    assert result["resource_configured"] is False
    assert result["verification_completed"] is False


def test_configured_source_is_not_claimed_as_executed(monkeypatch):
    monkeypatch.setenv("GENESIS_SECTOR_SOURCES_JSON", json.dumps({
        "FINANCE": {
            "source_name": "Official public site",
            "source_url": "https://example.org/public",
            "resource_name": "Public web adapter",
            "resource_type": "web"
        }
    }))
    result = sector_source_status("FINANCE")
    assert result["source_configured"] is True
    assert result["resource_configured"] is True
    assert result["worker_executed"] is False
    assert "example.org/public" in result["source"]


def test_invalid_source_cannot_be_presented_as_approved(monkeypatch):
    monkeypatch.setenv("GENESIS_SECTOR_SOURCES_JSON", json.dumps({
        "FINANCE": {"source_name": "Bad", "source_url": "http://127.0.0.1",
                    "resource_name": "Named", "resource_type": "unknown"}
    }))
    result = sector_source_status("FINANCE")
    assert result["source_configured"] is False
    assert result["resource_configured"] is False
