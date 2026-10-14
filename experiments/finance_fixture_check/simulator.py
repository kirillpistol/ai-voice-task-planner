"""Synthetic observations only; never opens a bank app or network connection."""
from .models import Fixture, Observation

FIXTURES = [
    Fixture("fixture-match", "Тестовый Получатель А."),
    Fixture("fixture-mismatch", "Тестовый Получатель Б."),
    Fixture("fixture-missing", "Тестовый Получатель В."),
]


class SimulatedAndroidAdapter:
    environment = "offline-simulation"
    def __init__(self):
        self.opened = False
        self.closed = False
    def open_session(self, session):
        self.opened = True
    def observe_fixture(self, case_id):
        if not self.opened or self.closed:
            raise RuntimeError("Session unavailable")
        return {
            "fixture-match": Observation(True, "Тестовый Получатель А."),
            "fixture-mismatch": Observation(True, "Другой Тестовый Получатель Б."),
            "fixture-missing": Observation(False, None),
        }[case_id]
    def close_session(self):
        self.closed = True
