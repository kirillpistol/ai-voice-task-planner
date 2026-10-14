from fastapi.testclient import TestClient
from app.main import app
from app.v2 import current_uid
from app import genesis_core_status as gateway


def test_status_requires_auth_and_does_not_enable_execution(monkeypatch):
    monkeypatch.delenv("GENESIS_CORE_STATUS_CONFIG", raising=False)
    try:
        with TestClient(app) as client:
            assert client.get("/api/v2/genesis/core/status").status_code == 401
            app.dependency_overrides[current_uid] = lambda: "alice"
            result = client.get("/api/v2/genesis/core/status").json()
            assert result["status"] == "not_configured"
            assert result["execution_allowed"] is False
            assert client.post("/api/v2/genesis/core/status", json={"path": "/v1/tasks"}).status_code == 405
            assert client.post("/api/v2/genesis/core/tasks", json={}).status_code == 404
    finally:
        app.dependency_overrides.clear()


def test_partial_and_invalid_core_responses_are_explicit(monkeypatch):
    monkeypatch.setattr(gateway, "load_config", lambda: ({}, None))
    def respond(origin, path, config, context):
        if origin.endswith("core-a:8443"):
            raise OSError("internal secret information must not be returned")
        return {"mode": "running", "metrics": {"completed": 4}, "ready": True,
                "algorithms": {"private-name": "v1"}}
    monkeypatch.setattr(gateway, "request", respond)
    result = gateway.collect_status()
    assert result["status"] == "partial"
    assert result["nodes"][1]["completed"] == 4
    assert "secret" not in str(result) and "private-name" not in str(result)
    monkeypatch.setattr(gateway, "request", lambda *args: {"mode": "running", "metrics": {"completed": True}, "ready": True})
    assert gateway.collect_status()["status"] == "unavailable"


def test_mutating_routes_and_arbitrary_origins_rejected_before_connect():
    import pytest
    with pytest.raises(ValueError):
        gateway.request("https://core-a:8443", "/v1/bindings/select", {}, None)
    with pytest.raises(ValueError):
        gateway.request("https://arbitrary.example", "/v1/status", {}, None)


def test_pin_is_checked_before_any_http_payload(monkeypatch):
    calls = []
    class Socket:
        def getpeercert(self, binary_form=False): return b"wrong certificate"
    class Connection:
        sock = Socket()
        def __init__(self, *args, **kwargs): pass
        def connect(self): calls.append("connect")
        def request(self, *args, **kwargs): calls.append("request")
        def close(self): calls.append("close")
    monkeypatch.setattr(gateway.http.client, "HTTPSConnection", Connection)
    import pytest
    with pytest.raises(ValueError):
        gateway.request("https://core-a:8443", "/v1/status",
                        {"peers": {"https://core-a:8443": ["0" * 64]}}, None)
    assert calls == ["connect", "close"]
