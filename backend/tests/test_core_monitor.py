import time
from app import genesis_core_status as gateway


def test_monitor_updates_without_mobile_requests_and_drops_connected_status(monkeypatch):
    monitor = gateway.CoreMonitor(interval=.001)
    calls = []
    def collect():
        calls.append(1)
        if len(calls) == 1:
            return {"status": "connected", "execution_allowed": False}
        monitor.stop_event.set()
        return {"status": "unavailable", "execution_allowed": False}
    monkeypatch.setattr(gateway, "collect_status", collect)
    monitor.start()
    monitor.thread.join(timeout=1)
    assert len(calls) == 2
    assert monitor.snapshot()["status"] == "unavailable"
    snapshot = monitor.snapshot()
    snapshot["status"] = "changed by caller"
    assert monitor.snapshot()["status"] == "unavailable"
    monitor.stop()
    assert monitor.snapshot() is None


def test_stale_status_is_not_reported_as_current():
    monitor = gateway.CoreMonitor()
    monitor.latest = {"status": "connected"}
    monitor.updated = time.monotonic() - 31
    assert monitor.snapshot() is None
