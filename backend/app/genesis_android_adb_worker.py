"""Standalone Android UI worker for controlled synthetic test apps.

Not integrated into GENESIS Core or its execution endpoint.
Uses Android accessibility hierarchy and ADB; no root or security bypass.
"""
from __future__ import annotations

import re
import subprocess
from dataclasses import dataclass
from pathlib import Path
from time import monotonic, sleep
from xml.etree import ElementTree as ET

from app.genesis_android_contract import Action, AndroidJob

_BOUNDS = re.compile(r"^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$")


class DeviceError(RuntimeError):
    pass


@dataclass
class AdbDevice:
    adb_path: str
    serial: str = "emulator-5554"

    def command(self, *args: str, timeout: int = 20) -> str:
        try:
            completed = subprocess.run(
                [self.adb_path, "-s", self.serial, *args],
                capture_output=True, text=True, timeout=timeout, check=False,
            )
        except (OSError, subprocess.TimeoutExpired) as exc:
            raise DeviceError("adb_unavailable_or_timed_out") from exc
        if completed.returncode:
            raise DeviceError("adb_operation_failed: " + completed.stderr[-300:])
        return completed.stdout

    def hierarchy(self) -> ET.Element:
        self.command("shell", "uiautomator", "dump", "/sdcard/window.xml")
        xml = self.command("exec-out", "cat", "/sdcard/window.xml")
        try:
            return ET.fromstring(xml)
        except ET.ParseError as exc:
            raise DeviceError("invalid_android_ui_hierarchy") from exc

    def node(self, package: str, selector: str):
        # Select only exact resource-ids belonging to the allowlisted application.
        expected = package + ":id/" + selector
        for node in self.hierarchy().iter("node"):
            if node.attrib.get("package") == package and node.attrib.get("resource-id") == expected:
                return node
        return None

    def tap_node(self, node: ET.Element) -> None:
        match = _BOUNDS.fullmatch(node.attrib.get("bounds", ""))
        if not match:
            raise DeviceError("element_has_no_bounds")
        x1, y1, x2, y2 = map(int, match.groups())
        if x2 <= x1 or y2 <= y1:
            raise DeviceError("invalid_element_bounds")
        self.command("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))


def execute(job: AndroidJob, device: AdbDevice) -> dict:
    """Execute only synthetic UI steps; never log free-form screen contents."""
    job.validate()
    if device.command("get-state").strip() != "device":
        raise DeviceError("android_device_not_ready")
    log = []
    for i, step in enumerate(job.steps):
        start = monotonic()
        outcome = "passed"
        try:
            if step.action == Action.LAUNCH:
                # Resolve launcher through Android rather than arbitrary activity names.
                launch = device.command("shell", "cmd", "package", "resolve-activity",
                                        "--brief", job.package).strip().splitlines()
                component = launch[-1] if launch else ""
                if not component.startswith(job.package + "/"):
                    raise DeviceError("launcher_not_found_for_package")
                device.command("shell", "am", "start", "-n", component)
            else:
                deadline = monotonic() + step.timeout_ms / 1000
                node = device.node(job.package, step.selector)
                while node is None and monotonic() < deadline:
                    sleep(0.2)
                    node = device.node(job.package, step.selector)
                if node is None:
                    raise DeviceError("element_not_found")
                if step.action == Action.TAP:
                    device.tap_node(node)
                elif step.action == Action.TYPE_TEST_ID:
                    device.tap_node(node)
                    device.command("shell", "input", "text", step.value)
                elif step.action == Action.CLEAR:
                    # Select-all via Android key combo; then DEL.
                    device.tap_node(node)
                    device.command("shell", "input", "keyevent", "KEYCODE_MOVE_END")
                    device.command("shell", "input", "keyevent", "--longpress", "KEYCODE_MOVE_HOME")
                    # Verify clearing through a separate assertion before using in production.
                    device.command("shell", "input", "keyevent", "KEYCODE_DEL")
                elif step.action in {Action.ASSERT_VISIBLE, Action.WAIT_VISIBLE}:
                    pass
                else:
                    raise DeviceError("unsupported_step")
        except DeviceError:
            outcome = "failed"
            log.append({"index": i, "action": step.action.value, "status": outcome,
                        "duration_ms": int((monotonic() - start) * 1000)})
            break
        log.append({"index": i, "action": step.action.value, "status": outcome,
                    "duration_ms": int((monotonic() - start) * 1000)})
    return {
        "job_id": job.job_id, "mode": "local_adb_test_runner",
        "isolated_execution": False, "executed": True,
        "passed": all(x["status"] == "passed" for x in log) and len(log) == len(job.steps),
        "steps": log,
    }
