"""Tests for phone-generation API behavior."""
from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_generates_single_number():
    response = client.get("/api/v1/tools/generate-phone")
    assert response.status_code == 200
    data = response.json()
    assert data["count"] == 1
    assert len(data["phones"]) == 1
    assert data["phones"][0].startswith("+7999")
    assert len(data["phones"][0]) == 12
    assert data["phones"][0][1:].isdigit()


def test_batch_has_unique_numbers_and_custom_prefix():
    response = client.get("/api/v1/tools/generate-phone", params={"prefix": "916", "count": 100})
    assert response.status_code == 200
    phones = response.json()["phones"]
    assert len(phones) == len(set(phones)) == 100
    assert all(phone.startswith("+7916") for phone in phones)


def test_rejects_invalid_input():
    for params in ({"prefix": "abc"}, {"prefix": "99"}, {"prefix": "000"},
                   {"count": 0}, {"count": 1001}):
        assert client.get("/api/v1/tools/generate-phone", params=params).status_code == 422
