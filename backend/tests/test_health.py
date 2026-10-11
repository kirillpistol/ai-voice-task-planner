from fastapi.testclient import TestClient

from app.main import app


def test_health():
    with TestClient(app) as client:
        result = client.get("/healthz")
    assert result.status_code == 200
    assert result.json()["status"] == "ok"


def test_bootstrap_contract_is_explicit():
    with TestClient(app) as client:
        result = client.get("/api/v1")
    assert result.status_code == 200
    assert result.json()["contract"] == "pending verification"
