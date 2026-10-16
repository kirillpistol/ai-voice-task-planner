from fastapi.testclient import TestClient
from app.genesis_task_intake_api import app


def test_import_process_observe_report(tmp_path, monkeypatch):
    monkeypatch.setenv("GENESIS_TASK_DB", str(tmp_path / "tasks.sqlite"))
    monkeypatch.setenv("GENESIS_REPORT_DIR", str(tmp_path / "reports"))
    monkeypatch.setenv("GENESIS_TASK_TOKEN", "local-test-secret")
    client = TestClient(app)
    headers = {"X-Genesis-Token": "local-test-secret"}
    resp = client.post("/v1/tasks/import", headers=headers, json={
        "csv_text": "record_id,scenario\ndemo-001,positive\ndemo-002,unknown\n"
    })
    assert resp.status_code == 200, resp.text
    dataset_id = resp.json()["dataset_id"]
    assert client.get(f"/v1/tasks/{dataset_id}/report", headers=headers).status_code == 404
    processed = client.post(f"/v1/tasks/{dataset_id}/process", headers=headers,
                            json={"confirm": True})
    assert processed.status_code == 200, processed.text
    assert processed.json()["count"] == 2
    report_response = client.get(f"/v1/tasks/{dataset_id}/report", headers=headers)
    assert report_response.status_code == 200
    report = report_response.json()
    assert report["records"][0]["result"]["stage"] == "data_validation"
    assert report["records"][0]["result"]["external_lookup_performed"] is False
    assert report["security"]["deployment_status"] == "not_deployed"
    assert (tmp_path / "reports" / f"{dataset_id}.json").exists()


def test_process_requires_auth(tmp_path, monkeypatch):
    monkeypatch.setenv("GENESIS_TASK_TOKEN", "secret")
    client = TestClient(app)
    assert client.post("/v1/tasks/" + "a" * 32 + "/process",
                       json={"confirm": True}).status_code == 401
