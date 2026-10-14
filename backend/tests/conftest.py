"""Backend tests must not discover cloud credentials or production databases."""
import pytest
from google.auth.exceptions import DefaultCredentialsError


@pytest.fixture(autouse=True)
def isolated_infrastructure(monkeypatch):
    def unavailable_firestore(*args, **kwargs):
        raise DefaultCredentialsError("Test environment: Firestore credentials intentionally unavailable")
    monkeypatch.setattr("app.main.firestore.Client", unavailable_firestore)
    monkeypatch.delenv("GENESIS_DATABASE_URL", raising=False)
    monkeypatch.delenv("GENESIS_DATABASE_URL_FILE", raising=False)
    monkeypatch.delenv("GENESIS_CORE_STATUS_CONFIG", raising=False)
