"""Server owns request metering; clients cannot submit counter values."""
from datetime import datetime, timezone
from sqlalchemy import create_engine
from sqlalchemy.orm import Session
from fastapi import HTTPException
import pytest
from app.genesis_store import Base
from app.genesis_ai import _charge_one, _read_usage


def test_atomic_quota_limit_and_user_separation(monkeypatch):
    monkeypatch.setenv("GENESIS_AI_MONTHLY_LIMIT", "2")
    engine = create_engine("sqlite+pysqlite:///:memory:")
    Base.metadata.create_all(engine)
    with Session(engine) as session:
        assert _read_usage("user-a", session)["used"] == 0
        _charge_one("user-a", session)
        _charge_one("user-a", session)
        assert _read_usage("user-a", session)["used"] == 2
        with pytest.raises(HTTPException) as err:
            _charge_one("user-a", session)
        assert err.value.status_code == 429
        assert _read_usage("user-b", session)["used"] == 0
