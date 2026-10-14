from dataclasses import dataclass
from enum import Enum
from typing import Protocol


class Verdict(str, Enum):
    MATCH = "match"
    MISMATCH = "mismatch"
    NOT_FOUND = "not_found"
    ERROR = "error"


@dataclass(frozen=True)
class AndroidSession:
    application: str
    account_ref: str
    manually_authenticated: bool


@dataclass(frozen=True)
class Fixture:
    case_id: str
    reference_name: str


@dataclass(frozen=True)
class Observation:
    found: bool
    displayed_name: str | None


@dataclass(frozen=True)
class Result:
    case_id: str
    verdict: Verdict
    reason: str


class Adapter(Protocol):
    environment: str
    def open_session(self, session: AndroidSession) -> None: ...
    def observe_fixture(self, case_id: str) -> Observation: ...
    def close_session(self) -> None: ...
