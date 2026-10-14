import importlib.util
import json
import os
from pathlib import Path

spec = importlib.util.spec_from_file_location("core_connect", Path(__file__).parents[1] / "deploy" / "connect-core-api.py")
connect = importlib.util.module_from_spec(spec)
spec.loader.exec_module(connect)


def test_gateway_secret_contains_only_data_identity(tmp_path):
    source = tmp_path / "circle" / "state" / "checker"
    source.mkdir(parents=True)
    for name in ("ca.crt", "data.crt", "data.key", "control.key"):
        (source / name).write_text("synthetic test content")
    (source / "data.json").write_text(json.dumps({"tls": {}, "peers": {
        "https://core-a:8443": ["0" * 64], "https://core-b:8443": ["1" * 64]}}))
    secret = connect.prepare_secret(tmp_path / "circle", tmp_path / "secret", os.getuid(), os.getgid())
    assert {p.name for p in secret.iterdir()} == {"ca.crt", "data.crt", "data.key", "client.json"}
    assert (secret / "data.key").stat().st_mode & 0o777 == 0o400
    config = json.loads((secret / "client.json").read_text())
    assert config["tls"]["key"] == "/run/secrets/genesis-core-status/data.key"


def test_private_network_required_before_api_changes(tmp_path, monkeypatch):
    import pytest
    (tmp_path / ".env").write_text("COMPOSE_PROJECT_NAME=genesis-circle-test\n")
    calls = []
    def docker(method, path, body=None):
        calls.append((method, path))
        if path == "/version": return {"ApiVersion": "1.50"}
        return {"Internal": False, "Labels": {}}
    monkeypatch.setattr(connect, "docker", docker)
    monkeypatch.setattr(connect.os, "geteuid", lambda: 0)
    with pytest.raises(ValueError):
        connect.connect(tmp_path, "image")
    assert all(method == "GET" for method, path in calls)
