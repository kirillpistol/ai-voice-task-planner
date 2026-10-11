from unittest.mock import patch

from fastapi.testclient import TestClient

from app.main import app
from app.v2 import current_uid


def test_auth_required():
    with TestClient(app) as client:
        assert client.get("/api/v2/session").status_code == 401
        assert client.get("/api/v2/tasks").status_code == 401
        assert client.post("/api/v2/tasks", json={"title": "Test"}).status_code == 401


def test_empty_bearer_token_is_rejected():
    with TestClient(app) as client:
        assert client.get("/api/v2/session", headers={"Authorization": "Bearer "}).status_code == 401


def test_valid_firebase_token_returns_uid():
    with patch("app.v2.firebase_admin.get_app"), patch(
        "app.v2.firebase_auth.verify_id_token", return_value={"uid": "firebase-user-123"}
    ) as verify:
        with TestClient(app) as client:
            response = client.get(
                "/api/v2/session",
                headers={"Authorization": "Bearer valid-test-token"},
            )
        assert response.status_code == 200
        assert response.json() == {"uid": "firebase-user-123"}
        verify.assert_called_once_with("valid-test-token", check_revoked=True)


def test_invalid_firebase_token_is_rejected():
    with patch("app.v2.firebase_admin.get_app"), patch(
        "app.v2.firebase_auth.verify_id_token", side_effect=ValueError("invalid")
    ):
        with TestClient(app) as client:
            response = client.get(
                "/api/v2/session",
                headers={"Authorization": "Bearer invalid-token"},
            )
        assert response.status_code == 401


def test_missing_uid_is_rejected():
    with patch("app.v2.firebase_admin.get_app"), patch(
        "app.v2.firebase_auth.verify_id_token", return_value={}
    ):
        with TestClient(app) as client:
            response = client.get(
                "/api/v2/session",
                headers={"Authorization": "Bearer no-user"},
            )
        assert response.status_code == 401
