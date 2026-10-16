"""GENESIS Level-3 Android test-task contract.

No application-specific workflows live in GENESIS Core. This module defines
a restrictive, permissioned test contract. It does not dispatch to a device.
"""
from dataclasses import dataclass, field
from enum import Enum
from typing import Any
from uuid import uuid4
import re

CASE_ID = re.compile(r"^demo-[0-9]{3}$")
APP_ID = re.compile(r"^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$")


class Action(str, Enum):
    LAUNCH = "launch"
    ASSERT_VISIBLE = "assert_visible"
    TAP = "tap"
    TYPE_TEST_ID = "type_test_id"
    CLEAR = "clear"
    WAIT_VISIBLE = "wait_visible"


@dataclass(frozen=True)
class AndroidStep:
    action: Action
    selector: str = ""
    value: str = ""
    timeout_ms: int = 5000

    def validate(self) -> None:
        if not 100 <= self.timeout_ms <= 15000:
            raise ValueError("step_timeout_out_of_range")
        if self.action in {Action.ASSERT_VISIBLE, Action.TAP, Action.CLEAR, Action.WAIT_VISIBLE, Action.TYPE_TEST_ID}:
            if not self.selector or len(self.selector) > 120:
                raise ValueError("selector_required")
        if self.action == Action.TYPE_TEST_ID and not CASE_ID.fullmatch(self.value):
            raise ValueError("only_synthetic_case_ids_allowed")
        if self.action != Action.TYPE_TEST_ID and self.value:
            raise ValueError("unexpected_value")


@dataclass(frozen=True)
class AndroidJob:
    package: str
    steps: tuple[AndroidStep, ...]
    approved_package: str
    test_scope: str = "synthetic_ui"
    job_id: str = field(default_factory=lambda: uuid4().hex)

    def validate(self) -> None:
        if not APP_ID.fullmatch(self.package) or self.package != self.approved_package:
            raise ValueError("package_not_allowlisted")
        if self.package == "ru.vtb24.mobilebanking.android":
            raise ValueError("bank_app_not_supported_by_test_runner")
        if self.test_scope != "synthetic_ui":
            raise ValueError("unsupported_test_scope")
        if not 1 <= len(self.steps) <= 40:
            raise ValueError("steps_out_of_range")
        if self.steps[0].action != Action.LAUNCH:
            raise ValueError("first_step_must_launch")
        for step in self.steps:
            step.validate()


def serialize_job(job: AndroidJob) -> dict[str, Any]:
    job.validate()
    return {
        "job_id": job.job_id,
        "mode": "permissioned_android_ui_test",
        "scope": job.test_scope,
        "package": job.package,
        "steps": [
            {"action": step.action.value, "selector": step.selector,
             "value": step.value, "timeout_ms": step.timeout_ms}
            for step in job.steps
        ],
        "isolated_execution": False,
        "dispatch_status": "not_configured",
    }
