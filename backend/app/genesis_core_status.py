"""Authenticated read-only status gateway. No task/control forwarding."""
import copy
import time
import concurrent.futures
import hashlib
import http.client
import json
import os
import re
import ssl
import threading
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlsplit

from fastapi import APIRouter, Depends
from app.v2 import current_uid

router = APIRouter(prefix="/api/v2/genesis/core", tags=["genesis-core-status"])
slots = threading.BoundedSemaphore(2)
ALLOWED = {"https://core-a:8443": "A", "https://core-b:8443": "B"}


def load_config():
    path = os.getenv("GENESIS_CORE_STATUS_CONFIG", "").strip()
    if not path:
        return None
    with open(path, "rb") as file:
        raw = file.read(65537)
    if len(raw) > 65536:
        raise ValueError("Configuration too large")
    config = json.loads(raw)
    if set(config["peers"]) != set(ALLOWED):
        raise ValueError("Only the two configured internal core origins are supported")
    for pins in config["peers"].values():
        if not isinstance(pins, list) or not pins or any(not isinstance(p, str) or not re.fullmatch("[0-9a-f]{64}", p) for p in pins):
            raise ValueError("Invalid certificate pin")
    ctx = ssl.create_default_context(cafile=config["tls"]["ca"])
    ctx.minimum_version = ssl.TLSVersion.TLSv1_2
    ctx.load_cert_chain(config["tls"]["cert"], config["tls"]["key"])
    return config, ctx


def request(origin, path, config, context):
    if origin not in ALLOWED or path not in ("/v1/status", "/v1/readiness"):
        raise ValueError("Gateway route is not permitted")
    address = urlsplit(origin)
    conn = http.client.HTTPSConnection(address.hostname, address.port, context=context, timeout=2)
    try:
        conn.connect()
        pin = hashlib.sha256(conn.sock.getpeercert(binary_form=True)).hexdigest()
        if pin not in config["peers"][origin]:
            raise ValueError("Peer identity mismatch")
        conn.request("GET", path, headers={"X-Correlation-ID": "app-core-status"})
        response = conn.getresponse()
        if response.status not in ((200, 503) if path == "/v1/readiness" else (200,)):
            raise ValueError("Core rejected status request")
        if response.getheader("Content-Type") != "application/json":
            raise ValueError("Expected JSON; redirects are not followed")
        raw = response.read(65537)
        if len(raw) > 65536:
            raise ValueError("Core response too large")
        result = json.loads(raw)
        if result.get("api_version") != "1.0":
            raise ValueError("Unsupported core contract")
        return result
    finally:
        conn.close()


def node(origin, config, context):
    try:
        status = request(origin, "/v1/status", config, context)
        readiness = request(origin, "/v1/readiness", config, context)
        mode, completed, ready = status["mode"], status["metrics"]["completed"], readiness["ready"]
        if mode not in ("running", "paused") or type(completed) is not int or completed < 0 or type(ready) is not bool:
            raise ValueError("Invalid core status")
        # Never expose addresses, certificates, snapshots, algorithms or user data.
        return {"name": ALLOWED[origin], "reachable": True, "mode": mode, "ready": ready, "completed": completed}
    except Exception:
        return {"name": ALLOWED[origin], "reachable": False, "mode": "unavailable", "ready": False, "completed": 0}


def collect_status():
    result = {"status": "not_configured", "nodes": [], "execution_allowed": False,
              "read_only": True, "checked_at": datetime.now(timezone.utc).isoformat()}
    if not slots.acquire(blocking=False):
        return {**result, "status": "busy"}
    try:
        try:
            loaded = load_config()
            if loaded is None:
                return result
            config, context = loaded
            with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
                nodes = list(pool.map(lambda origin: node(origin, config, context), ALLOWED))
            connected = sum(value["reachable"] for value in nodes)
            return {**result, "nodes": nodes,
                    "status": "connected" if connected == 2 else "partial" if connected == 1 else "unavailable"}
        except Exception:
            return {**result, "status": "unavailable"}
    finally:
        slots.release()


class CoreMonitor:
    """Keep observing the private cores independently of mobile app lifecycle."""
    def __init__(self, interval=10):
        self.interval = interval
        self.stop_event = threading.Event()
        self.lock = threading.Lock()
        self.thread = None
        self.latest = None
        self.updated = 0.0

    def start(self):
        if self.thread is not None and self.thread.is_alive():
            return
        self.stop_event.clear()
        self.thread = threading.Thread(target=self.run, name="genesis-core-monitor", daemon=True)
        self.thread.start()

    def run(self):
        while not self.stop_event.is_set():
            result = collect_status()
            with self.lock:
                self.latest = result
                self.updated = time.monotonic()
            self.stop_event.wait(self.interval)

    def snapshot(self):
        with self.lock:
            if self.latest is None or time.monotonic() - self.updated > 30:
                return None
            return copy.deepcopy(self.latest)

    def stop(self):
        self.stop_event.set()
        if self.thread is not None:
            self.thread.join(timeout=10)
        with self.lock:
            self.latest = None


monitor = CoreMonitor()


@router.get("/status")
def status(uid: str = Depends(current_uid)):
    return monitor.snapshot() or collect_status()
