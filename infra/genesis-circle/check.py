"""Real mTLS L3 -> L1 -> L2 loop, handoff and negative access checks."""
import argparse
import copy
import http.client
import json
import math
import time
from pathlib import Path

from level1_core.handoff import transfer
from level1_core.transport import Client, PeerError
from level3_data.bound import BoundAdapter, PartClient


def client(document):
    return Client(**document["tls"], peers=document["peers"], timeout=document.get("timeout_s", 3))


def rejected(action, status=None):
    try:
        action()
    except PeerError as error:
        if status is not None and error.status != status:
            raise AssertionError("Unexpected rejection status")
        return
    except (ValueError, OSError, http.client.HTTPException):
        if status is not None:
            raise
        return
    raise AssertionError("Forbidden operation was accepted")


def run(directory):
    root = Path(directory)
    def load(name):
        return json.loads((root / name).read_text())
    config = load("control.json")
    control, data, outsider = client(config), client(load("data.json")), client(load("outsider.json"))
    a, b = config["endpoints"]
    parts_config = load("parts.json")
    parts = PartClient(**parts_config["tls"], peers=parts_config["peers"])
    manifest, passport = load("manifest.json"), load("passport.json")
    deadline = time.monotonic() + 30
    while True:
        try:
            for url in (a, b):
                if control.request(url, "/v1/health")["alive"] is not True:
                    raise ValueError("Core health check failed")
            for part in manifest["parts"]:
                parts.fetch(part)
            break
        except (OSError, ValueError, http.client.HTTPException):
            if time.monotonic() >= deadline:
                raise RuntimeError("Circle startup timeout")
            time.sleep(.5)
    for url in (a, b):
        status = control.request(url, "/v1/status")
        if status["metrics"]["completed"] != 0 or status["algorithms"]:
            raise ValueError("Use fresh cores: restart core-a and core-b before repeating this test")
    request = {"api_version": "1.0", "binding_id": passport["binding_id"]}
    rejected(lambda: data.request(a, "/v1/bindings/select", request), 403)
    rejected(lambda: outsider.request(a, "/v1/health"))
    bad_pin = copy.deepcopy(config)
    bad_pin["peers"][a] = ["0" * 64]
    rejected(lambda: client(bad_pin).request(a, "/v1/health"))
    wrong_source = PartClient(**config["tls"], peers=parts_config["peers"])
    rejected(lambda: wrong_source.fetch(manifest["parts"][0]))
    control.request(a, "/v1/bindings/select", request)
    adapter_a = BoundAdapter(data, a, passport, manifest, parts)
    first_message = adapter_a.message(manifest["parts"][0], 0, 10)
    rejected(lambda: control.request(a, "/v1/bindings/tasks", first_message), 403)
    rejected(lambda: data.request(a, "/v1/tasks", {"api_version": "1.0", "algorithm": "sector-a.numeric", "data": 10}), 400)
    stream = adapter_a.deliver(correlation_id="circle-first-half")
    first = [next(stream), next(stream)]
    stream.close()
    if first[-1]["result"]["count"] != 2 or first[-1]["result"]["mean"] != 15:
        raise AssertionError("First-half aggregation mismatch")
    before = data.request(a, "/v1/bindings/status")["binding"]
    if before["next_record"] != 2:
        raise AssertionError("Unconfirmed source cursor")
    transfer(control, a, b, correlation_id="circle-handoff")
    after = data.request(b, "/v1/bindings/status")["binding"]
    if before != after:
        raise AssertionError("Binding/cursor did not survive handoff")
    adapter_b = BoundAdapter(data, b, passport, manifest, parts)
    results = list(adapter_b.deliver(correlation_id="circle-second-half"))
    if len(results) != 2:
        raise AssertionError("Unexpected number of remaining records")
    result = results[-1]["result"]
    if result["count"] != 4 or result["mean"] != 25 or not math.isclose(result["sample_variance"], 500 / 3):
        raise AssertionError("Full-circle numeric result mismatch")
    if list(adapter_b.deliver()) != []:
        raise AssertionError("Completed stream was processed twice")
    rejected(lambda: data.request(b, "/v1/bindings/tasks", first_message), 400)
    status_a, status_b = control.request(a, "/v1/status"), control.request(b, "/v1/status")
    if status_a["mode"] != "paused" or status_b["metrics"]["completed"] != 4:
        raise AssertionError("Unexpected handoff or processing state")
    return {"status": "passed", "synthetic_data": True, "records": 4, "result": result,
            "handoff": "worker-a -> worker-b", "confirmed_cursor": 4,
            "repeat_records": 0, "mtls_roles_pins_and_binding_checks": "passed",
            "model_training": False, "application_connected": False}


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--directory", default="/run/genesis")
    args = parser.parse_args()
    try:
        print(json.dumps(run(args.directory), ensure_ascii=False, indent=2))
    except Exception as error:
        raise SystemExit("Circle check failed: " + type(error).__name__ + ". Inspect the test container logs; do not paste keys.")
