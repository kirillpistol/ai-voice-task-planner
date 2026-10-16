import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.main import app
from app.genesis_store import Base, require_sql_session
from app.genesis_memory import MemoryRule, MAX_RULES
from app.v2 import current_uid


@pytest.fixture
def client(monkeypatch):
    monkeypatch.setenv("GENESIS_MEMORY_PRIVACY_URL", "https://example.org/memory-v1")
    monkeypatch.setenv("GENESIS_MEMORY_POLICY_VERSION", "memory-v1")
    monkeypatch.delenv("GENESIS_DATABASE_URL", raising=False)
    monkeypatch.delenv("GENESIS_DATABASE_URL_FILE", raising=False)
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool)
    Base.metadata.create_all(engine)
    factory = sessionmaker(bind=engine, expire_on_commit=False)
    def session():
        with factory() as db:
            yield db
    app.dependency_overrides[require_sql_session] = session
    app.dependency_overrides[current_uid] = lambda: "alice"
    try:
        with TestClient(app) as value:
            app.state.genesis_sql_factory = factory
            yield value
    finally:
        app.dependency_overrides.clear()
        engine.dispose()


def comment(**extra):
    return {"sector": "LOGISTICS", "kind": "constraint", "comment": "Не ставить сложные задачи после работы", **extra}


def confirm(client, **extra):
    return client.post("/api/v2/genesis/memory/confirm", json=comment(
        policy_version="memory-v1", consent_to_store=True, **extra))


def test_preview_does_not_learn_and_confirm_changes_next_context(client):
    path = "/api/v2/genesis/memory"
    preview = client.post(path + "/preview", json=comment()).json()
    assert preview["status"] == "preview" and preview["rules"] == []
    assert preview["execution_allowed"] is False
    assert client.get(path + "/LOGISTICS").json()["rules"] == []
    saved = confirm(client).json()
    assert saved["status"] == "confirmed" and saved["consent_accepted"]
    assert len(saved["rules"]) == 1
    next_request = client.post(path + "/preview", json=comment(comment="Сначала проверить срок поставки")).json()
    assert next_request["rules"] == saved["rules"]
    assert saved["rules"][0]["text"] in next_request["text"]
    assert len(confirm(client, comment="  Не ставить   сложные задачи после работы  ").json()["rules"]) == 1
    rule = saved["rules"][0]["id"]
    assert client.delete(path + "/LOGISTICS/" + rule).status_code == 204
    assert client.get(path + "/LOGISTICS").json()["rules"] == []


def test_account_sector_isolation_and_unconfirmed_input(client):
    saved = confirm(client).json()
    rule = saved["rules"][0]["id"]
    assert client.get("/api/v2/genesis/memory/FINANCE").json()["rules"] == []
    app.dependency_overrides[current_uid] = lambda: "bob"
    assert client.get("/api/v2/genesis/memory/LOGISTICS").json()["rules"] == []
    assert client.delete("/api/v2/genesis/memory/LOGISTICS/" + rule).status_code == 404
    app.dependency_overrides.pop(current_uid)
    assert client.post("/api/v2/genesis/memory/preview", json=comment()).status_code == 401


def test_policy_change_requires_new_consent(client, monkeypatch):
    path = "/api/v2/genesis/memory/confirm"
    assert client.post(path, json=comment(policy_version="memory-v1")).status_code == 400
    assert confirm(client).status_code == 200
    # Existing consent permits another confirmation without repeating the checkbox.
    assert client.post(path, json=comment(comment="Новое правило", policy_version="memory-v1")).status_code == 200
    monkeypatch.setenv("GENESIS_MEMORY_POLICY_VERSION", "memory-v2")
    assert client.get("/api/v2/genesis/memory/LOGISTICS").json()["consent_accepted"] is False
    assert client.post(path, json=comment(policy_version="memory-v2")).status_code == 400
    assert confirm(client).status_code == 400
    monkeypatch.delenv("GENESIS_MEMORY_PRIVACY_URL")
    assert client.post("/api/v2/genesis/memory/preview", json=comment()).status_code == 200
    # Preview reads existing tasks only; publishing a policy is required for saving rules.
    assert client.post(path, json=comment(policy_version="memory-v2", consent_to_store=True)).status_code == 503
    # Policy/configuration changes must not prevent reading or erasing stored data.
    data = client.get("/api/v2/genesis/memory/LOGISTICS").json()
    assert client.delete("/api/v2/genesis/memory/LOGISTICS/" + data["rules"][0]["id"]).status_code == 204


@pytest.mark.parametrize("extra", [
    {"comment": " "}, {"comment": "a" * 2001}, {"comment": "bad\u0000text"},
    {"sector": "UNKNOWN"}, {"kind": "execute"}, {"user_id": "bob"},
])
def test_reject_invalid_requests(client, extra):
    assert client.post("/api/v2/genesis/memory/preview", json=comment(**extra)).status_code == 422


def test_limit_blocks_new_rule_but_allows_retry(client):
    for i in range(MAX_RULES):
        assert confirm(client, comment=str(i)).status_code == 200
    assert confirm(client, comment="new").status_code == 409
    assert confirm(client, comment="0").status_code == 200
