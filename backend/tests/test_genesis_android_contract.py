"""Contract tests; no emulator or third-party app access."""
import pytest
from app.genesis_android_contract import Action, AndroidJob, AndroidStep, serialize_job


def _job(*steps, package="org.genesis.fixture"):
    return AndroidJob(package=package, approved_package=package, steps=steps)


def test_valid_job_serializes_without_claiming_execution():
    result = serialize_job(_job(
        AndroidStep(Action.LAUNCH),
        AndroidStep(Action.TYPE_TEST_ID, selector="case_input", value="demo-001"),
        AndroidStep(Action.ASSERT_VISIBLE, selector="result_positive"),
        AndroidStep(Action.CLEAR, selector="case_input"),
    ))
    assert result["dispatch_status"] == "not_configured"
    assert result["isolated_execution"] is False
    assert len(result["steps"]) == 4


@pytest.mark.parametrize("input_value", ["+79001234567", "1234567890", "demo-001;rm -rf /", ""])
def test_nonfixture_input_rejected(input_value):
    with pytest.raises(ValueError, match="only_synthetic_case_ids_allowed"):
        serialize_job(_job(AndroidStep(Action.LAUNCH),
                           AndroidStep(Action.TYPE_TEST_ID, selector="case_input", value=input_value)))


def test_allowlist_mismatch_rejected():
    with pytest.raises(ValueError, match="package_not_allowlisted"):
        serialize_job(AndroidJob(package="org.other.app", approved_package="org.genesis.fixture",
                                 steps=(AndroidStep(Action.LAUNCH),)))


def test_bank_app_rejected():
    with pytest.raises(ValueError, match="bank_app_not_supported"):
        serialize_job(_job(AndroidStep(Action.LAUNCH), package="ru.vtb24.mobilebanking.android"))


def test_requires_launch_and_bounded_steps():
    with pytest.raises(ValueError, match="first_step_must_launch"):
        serialize_job(_job(AndroidStep(Action.TAP, selector="go")))
    with pytest.raises(ValueError, match="step_timeout_out_of_range"):
        serialize_job(_job(AndroidStep(Action.LAUNCH, timeout_ms=90000)))
