"""Bounded, approved fixture execution. No number enumeration or transfer actions."""
import unicodedata
from .models import Adapter, AndroidSession, Fixture, Observation, Result, Verdict


def normalize(value):
    return " ".join(unicodedata.normalize("NFKC", value).split()).casefold()


def compare(fixture: Fixture, observed: Observation) -> Result:
    if not observed.found or not observed.displayed_name or not observed.displayed_name.strip():
        return Result(fixture.case_id, Verdict.NOT_FOUND, "no_recipient_label")
    same = normalize(fixture.reference_name) == normalize(observed.displayed_name)
    return Result(fixture.case_id, Verdict.MATCH if same else Verdict.MISMATCH,
                  "reference_matches" if same else "reference_differs")


def run(adapter: Adapter, session: AndroidSession, fixtures: list[Fixture], approved_ids: set[str]):
    # Validate the entire suite before opening an application/session.
    if adapter.environment != "offline-simulation":
        raise ValueError("Live Android adapter requires a separate approved implementation")
    if not session.manually_authenticated or not session.account_ref.strip():
        raise ValueError("Explicit authenticated test session required")
    if not 1 <= len(fixtures) <= 32:
        raise ValueError("Expected 1 to 32 declared fixtures")
    ids = [fixture.case_id for fixture in fixtures]
    if len(ids) != len(set(ids)) or any(identifier not in approved_ids for identifier in ids):
        raise ValueError("Duplicate or unapproved fixture")
    if any(not fixture.reference_name.strip() for fixture in fixtures):
        raise ValueError("An explicit comparison reference is required")
    results = []
    try:
        adapter.open_session(session)
        for fixture in fixtures:
            try:
                results.append(compare(fixture, adapter.observe_fixture(fixture.case_id)))
            except Exception:
                # Do not leak interface text, credentials or exception payloads.
                results.append(Result(fixture.case_id, Verdict.ERROR, "adapter_error"))
        return results
    finally:
        adapter.close_session()
