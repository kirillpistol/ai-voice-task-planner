import json

from app.genesis_source_registry import sector_source_status


def test_public_receipt_populates_existing_cards(monkeypatch, tmp_path):
    monkeypatch.setenv("GENESIS_PUBLIC_REPORT_DIR", str(tmp_path))
    monkeypatch.setenv("GENESIS_SECTOR_SOURCES_JSON", json.dumps({
        "FINANCE": {"source_name": "Example public site",
                    "source_url": "https://www.example.com/",
                    "resource_name": "Server HTTPS checker", "resource_type": "web"}
    }))
    (tmp_path / "FINANCE.json").write_text(json.dumps({
        "sector": "FINANCE", "source_url": "https://www.example.com/",
        "status": "verified_http_200", "verified_at": "2026-10-16T12:00:00+00:00",
        "sha256": "a" * 64
    }), encoding="utf-8")
    value = sector_source_status("FINANCE")
    assert value["verification_completed"] is True
    assert "HTTP 200" in value["source"]
    assert "aaaaaaaa" in value["connection_resource"]


def test_unmatched_receipt_is_not_claimed(monkeypatch, tmp_path):
    monkeypatch.setenv("GENESIS_PUBLIC_REPORT_DIR", str(tmp_path))
    monkeypatch.setenv("GENESIS_SECTOR_SOURCES_JSON", json.dumps({
        "FINANCE": {"source_name": "Example", "source_url": "https://www.example.com/"}
    }))
    (tmp_path / "FINANCE.json").write_text(json.dumps({
        "source_url": "https://another.example/", "status": "verified_http_200"
    }), encoding="utf-8")
    assert sector_source_status("FINANCE")["verification_completed"] is False
