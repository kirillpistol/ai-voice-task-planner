from fastapi.testclient import TestClient

from app.main import app
from app.v2 import current_uid, database


class FakeDocument:
    def __init__(self):
        self.exists = False
        self.data = None
        self.id = "test-id"

    def set(self, data):
        self.exists = True
        self.data = data

    def get(self):
        return self

    def to_dict(self):
        return self.data

    def update(self, data):
        self.data.update(data)

    def delete(self):
        self.exists = False


class FakeCollection:
    def __init__(self):
        self.doc = FakeDocument()

    def document(self, name):
        return self.doc

    def limit(self, amount):
        return self

    def stream(self):
        return [self.doc] if self.doc.exists else []


class FakeDB:
    def __init__(self):
        self.items = FakeCollection()

    def collection(self, name):
        return self

    def document(self, name):
        return self

    def collection(self, name):
        return self.items if name == "tasks" else self


def test_missing_auth_rejected():
    with TestClient(app) as client:
        assert client.get("/api/v2/session").status_code == 401


def test_tasks_are_authenticated():
    fake = FakeDB()
    app.dependency_overrides[current_uid] = lambda: "test-user"
    app.dependency_overrides[database] = lambda: fake
    try:
        with TestClient(app) as client:
            response = client.post("/api/v2/tasks", json={"title": "First task"})
            assert response.status_code == 201, response.text
            task = response.json()
            assert task["title"] == "First task"
            assert client.get("/api/v2/tasks").json()[0]["id"] == task["id"]
            assert client.delete("/api/v2/tasks/" + task["id"]).status_code == 204
            assert client.get("/api/v2/tasks").json() == []
    finally:
        app.dependency_overrides.clear()
