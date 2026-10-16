"""Execution routing must never silently fall back to ordinary API traffic."""
from app.genesis_security_profiles import plan_execution, selected_profile


def test_finance_test_tool_is_blocked_until_deployed():
    result = plan_execution("FINANCE", "fixture_lookup")
    assert result["status"] == "blocked"
    assert result["execution_allowed"] is False
    assert result["profile_id"] == "vpn-vm-ephemeral-container-vpn"


def test_unassigned_sector_cannot_dispatch():
    result = plan_execution("MEDICINE", "fixture_lookup")
    assert result["status"] == "rejected"
    assert result["reason"] == "no_security_profile"


def test_unknown_tool_is_rejected():
    result = plan_execution("FINANCE", "arbitrary_command")
    assert result["status"] == "rejected"
    assert result["reason"] == "tool_not_allowed"


def test_profile_immutable_across_reads():
    profile = selected_profile("FINANCE")
    profile["deployment_status"] = "deployed"
    assert plan_execution("FINANCE", "ui_smoke_test")["execution_allowed"] is False


def test_even_declared_deployment_requires_worker_dispatch(monkeypatch):
    from app import genesis_security_profiles as profiles
    monkeypatch.setitem(profiles.SECURITY_PROFILES["vpn-vm-ephemeral-container-vpn"], "deployment_status", "deployed")
    result = profiles.plan_execution("FINANCE", "fixture_lookup")
    assert result["status"] == "blocked"
    assert result["reason"] == "worker_dispatch_not_configured"
