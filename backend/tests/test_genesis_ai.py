"""Offline tests: no OpenAI key or network required."""
from fastapi import HTTPException
from pydantic import ValidationError
import pytest

from app.genesis_ai import CommentAnalysisRequest, _allow, _hits, _lock


def test_external_processing_consent_required_by_endpoint_contract():
    req = CommentAnalysisRequest(sector="GENESIS", comment="Call supplier", consent_to_external_processing=False)
    assert req.consent_to_external_processing is False


def test_rejects_empty_or_excessive_comment():
    for text in ("", "x" * 4001):
        with pytest.raises(ValidationError):
            CommentAnalysisRequest(sector="GENESIS", comment=text, consent_to_external_processing=True)


def test_hourly_limit_enforced():
    uid = "test-genesis-ai-rate-user"
    with _lock:
        _hits.pop(uid, None)
    try:
        assert all(_allow(uid) for _ in range(10))
        assert not _allow(uid)
    finally:
        with _lock:
            _hits.pop(uid, None)
