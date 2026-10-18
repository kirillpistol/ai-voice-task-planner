"""Server owns request metering; clients cannot submit counter values."""
from datetime import datetime, timezone
from sqlalchemy import create_engine
from sqlalchemy.orm import Session
from fastapi import HTTPException
import pytest
from app.genesis_store import Base, GenesisAiUsageEvent, GenesisAiQuota
from app.genesis_ai import _charge_one, _read_usage, _settle_usage, CommentAnalysisResponse


def test_atomic_quota_limit_and_user_separation(monkeypatch):
    # Emulate 99 prior requests; the remaining Free allowance is one request.
    engine = create_engine("sqlite+pysqlite:///:memory:")
    Base.metadata.create_all(engine)
    with Session(engine) as session:
        assert _read_usage("user-a", session)["used"] == 0
        period = _read_usage("user-a", session)["period"]
        session.add(GenesisAiQuota(user_id="user-a", period=period, used=99))
        session.commit()
        _charge_one("user-a", session)
        assert _read_usage("user-a", session)["used"] == 100
        with pytest.raises(HTTPException) as err:
            _charge_one("user-a", session)
        assert err.value.status_code == 429
        assert _read_usage("user-b", session)["used"] == 0


def test_usage_ledger_settles_model_tokens(monkeypatch):
    monkeypatch.setenv("GENESIS_AI_MONTHLY_LIMIT", "100")
    engine = create_engine("sqlite+pysqlite:///:memory:")
    Base.metadata.create_all(engine)
    with Session(engine) as session:
        event_id = _charge_one("account-1", session)
        record = session.get(GenesisAiUsageEvent, event_id)
        assert record.status == "reserved"
        _settle_usage(event_id, session, result=CommentAnalysisResponse(
            summary="ok", tasks=[], input_tokens=42, output_tokens=8, model="fixture-model"))
        session.refresh(record)
        assert (record.status, record.input_tokens, record.output_tokens) == ("completed", 42, 8)
        _settle_usage(event_id, session, failed=True)
        session.refresh(record)
        assert record.status == "completed"


def test_repeated_operation_cannot_double_charge_monthly_allowance():
    engine = create_engine("sqlite+pysqlite:///:memory:")
    Base.metadata.create_all(engine)
    operation_id = "5f641ecd-ccfd-4909-bd35-82c44177cf93"
    with Session(engine) as session:
        assert _charge_one("alice", session, operation_id) == operation_id
        assert _read_usage("alice", session)["used"] == 1
        with pytest.raises(HTTPException) as error:
            _charge_one("alice", session, operation_id)
        assert error.value.status_code == 409
        assert _read_usage("alice", session)["used"] == 1
        with pytest.raises(HTTPException) as other_account:
            _charge_one("bob", session, operation_id)
        assert other_account.value.status_code == 409
        assert _read_usage("bob", session)["used"] == 0
