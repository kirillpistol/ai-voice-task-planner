import copy
import importlib.util
from pathlib import Path

import pytest

spec = importlib.util.spec_from_file_location("memory_deploy", Path(__file__).parents[1] / "deploy" / "update-memory-api.py")
deploy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(deploy)


def old_container():
    return {
        "Id": "old", "State": {"Running": True},
        "Config": {"Image": "original", "Env": ["EXISTING=keep", "GENESIS_PRIVACY_URL=https://example.org/calendar"],
                   "Cmd": ["uvicorn", "app.main:app"], "Labels": {"owner": "genesis"}},
        "HostConfig": {"AutoRemove": False, "RestartPolicy": {"Name": "unless-stopped"},
                       "Binds": ["/secrets/firebase:/run/secrets/firebase:ro"],
                       "PortBindings": {"8080/tcp": [{"HostIp": "127.0.0.1", "HostPort": "8080"}]}},
        "NetworkSettings": {"Networks": {"bridge": {}, "genesis-sql": {"Aliases": []}}},
    }


def test_replacement_preserves_production_and_does_not_mutate_old():
    old = old_container()
    before = copy.deepcopy(old)
    payload = deploy.replacement(old, "new-image", "https://example.org/memory", "v1")
    assert old == before
    assert payload["HostConfig"] == before["HostConfig"]
    assert payload["Cmd"] == before["Config"]["Cmd"]
    assert set(payload["Env"]) >= set(before["Config"]["Env"])
    assert set(payload["NetworkingConfig"]["EndpointsConfig"]) == {"bridge", "genesis-sql"}
    old["HostConfig"]["AutoRemove"] = True
    with pytest.raises(ValueError):
        deploy.replacement(old, "new", "https://example.org/memory", "v1")


def test_readiness_failure_restores_original_container(monkeypatch):
    calls = []
    def docker(method, path, body=None):
        calls.append((method, path, body))
        if path == "/version":
            return {"ApiVersion": "1.50"}
        if "/images/" in path:
            return {"Id": "new-image"}
        if path.endswith("genesis-api/json"):
            return old_container()
        if "/containers/create" in path:
            return {"Id": "candidate"}
    class Page:
        def __enter__(self): return self
        def __exit__(self, *args): pass
        def read(self): return b"v1</html>"
    monkeypatch.setattr(deploy, "docker", docker)
    monkeypatch.setattr(deploy.urllib.request, "urlopen", lambda *a, **k: Page())
    monkeypatch.setattr(deploy, "get_json", lambda url: (_ for _ in ()).throw(RuntimeError("not ready")))
    ticks = iter([0, 61])
    monkeypatch.setattr(deploy.time, "monotonic", lambda: next(ticks))
    with pytest.raises(RuntimeError):
        deploy.deploy("new", "https://example.org/memory", "v1")
    assert ("DELETE", "/v1.50/containers/candidate?force=true", None) in calls
    assert calls[-1] == ("POST", "/v1.50/containers/old/start", None)
    assert ("POST", "/v1.50/containers/old/rename?name=genesis-api", None) in calls
