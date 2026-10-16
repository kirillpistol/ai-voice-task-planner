"""GENESIS end-to-end smoke test on Android's built-in Settings app.

This is an actual ADB interaction, not a mocked device execution. The
SQLite observation is written only after checking foreground activity.
"""
from __future__ import annotations

import argparse
import json
import subprocess
from datetime import datetime, timezone
from pathlib import Path
from time import monotonic

from app.genesis_dataset_store import DatasetStore

SETTINGS_PACKAGE = "com.android.settings"


def adb_call(adb: str, serial: str, *args: str) -> str:
    run = subprocess.run([adb, "-s", serial, *args], capture_output=True,
                         text=True, encoding="utf-8", errors="replace",
                         timeout=30, check=False)
    if run.returncode:
        raise RuntimeError("adb_failed: " + run.stderr.strip()[-400:])
    return run.stdout.strip()


def smoke_test(adb: str, serial: str = "emulator-5554") -> dict:
    started = monotonic()
    if adb_call(adb, serial, "get-state") != "device":
        raise RuntimeError("device_not_ready")
    if adb_call(adb, serial, "shell", "getprop", "sys.boot_completed") != "1":
        raise RuntimeError("android_not_booted")
    launch_output = adb_call(adb, serial, "shell", "am", "start",
                             "-a", "android.settings.SETTINGS")
    # Foreground check via system activity service: this observes Android itself.
    import time
    focused = False
    activity_log = ""
    try:
        for _ in range(10):
            activity_log = adb_call(adb, serial, "shell", "dumpsys", "activity", "activities")
            focused_lines = [line for line in activity_log.splitlines()
                             if "topResumedActivity" in line or "mResumedActivity" in line]
            if any(SETTINGS_PACKAGE in line for line in focused_lines):
                focused = True
                break
            time.sleep(0.3)
    finally:
        adb_call(adb, serial, "shell", "input", "keyevent", "KEYCODE_HOME")
    return {
        "application": SETTINGS_PACKAGE,
        "action": "open_settings_verify_foreground_return_home",
        "status": "passed" if focused else "failed",
        "verified_foreground": focused,
        "duration_ms": round((monotonic() - started) * 1000, 2),
        "device": serial,
        "observed_at": datetime.now(timezone.utc).isoformat(),
    }


def run_e2e(adb: str, output_dir: str | Path, serial: str = "emulator-5554") -> dict:
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    db = DatasetStore(output_dir / "genesis_e2e.sqlite")
    template = db.register_template("ANDROID", "system_smoke",
                                    ["record_id", "scenario"])
    batch = db.import_csv(template["template_id"],
                          "record_id,scenario\ndemo-001,settings_launch\n")
    try:
        result = smoke_test(adb, serial)
    except (RuntimeError, subprocess.TimeoutExpired, OSError) as exc:
        result = {"application": SETTINGS_PACKAGE, "status": "error",
                  "error_type": type(exc).__name__, "message": str(exc)[-400:],
                  "device": serial}
    db.record_observation(batch["dataset_id"], "demo-001", SETTINGS_PACKAGE,
                          "system_smoke_v1", result)
    comparison = db.compare(batch["dataset_id"])
    report = {"execution_mode": "real_android_adb", "isolated_execution": False,
              "database_path": str(output_dir / "genesis_e2e.sqlite"),
              "dataset_id": batch["dataset_id"], "observation": result,
              "comparison": comparison}
    (output_dir / "genesis_e2e_report.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    return report


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="GENESIS Android + DB end-to-end smoke test")
    parser.add_argument("--adb", required=True, help="Path to adb executable")
    parser.add_argument("--serial", default="emulator-5554")
    parser.add_argument("--output", default="genesis_e2e_output")
    args = parser.parse_args()
    result = run_e2e(args.adb, args.output, args.serial)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    raise SystemExit(0 if result["observation"]["status"] == "passed" else 1)
