import os
import pytest
from fastapi.testclient import TestClient
from app.genesis_task_intake_api import app


@pytest.fixture
def client(tmp_path, monkeypatch):
    monkeypatch.setenv("GENESIS_TASK_DB", str(tmp_path / "test.sqlite"))
    monkeypatch.setenv("GENESIS_TASK_TOKEN", "test-secret-only")
    return TestClient(app)


def test_generate_validate_and_fetch(client):
    auth = {"X-Genesis-Token": "test-secret-only"}
    res = client.post("/v1/tasks/generate", json={"count": 3}, headers=auth)
    assert res.status_code == 200, res.text
    data = res.json()
    assert data["validation"] == "passed"
    assert [x["record_id"] for x in data["tasks"]] == ["demo-001", "demo-002", "demo-003"]
    fetched = client.get("/v1/tasks/" + data["dataset_id"], headers=auth)
    assert fetched.status_code == 200
    assert len(fetched.json()["tasks"]) == 3


def test_import_and_validation(client):
    auth = {"X-Genesis-Token": "test-secret-only"}
    data = "record_id,scenario\ndemo-003,positive\ndemo-007,negative\n"
    res = client.post("/v1/tasks/import", json={"csv_text": data}, headers=auth)
    assert res.status_code == 200
    assert res.json()["source"] == "uploaded"
    assert client.post("/v1/tasks/import",
                       json={"csv_text": "record_id,scenario\n+79990000000,positive\n"},
                       headers=auth).status_code == 422


def test_no_token_denied(client):
    assert client.post("/v1/tasks/generate", json={"count": 1}).status_code == 401
