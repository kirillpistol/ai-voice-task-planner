"""Developer-defined sector profiles; selection is separate from deployment."""
from copy import deepcopy

SECURITY_PROFILES = {
    "vpn-vm-ephemeral-container-vpn": {
        "id": "vpn-vm-ephemeral-container-vpn",
        "version": 1,
        "applies_to": "sector_services",
        "stages": ["inbound_vpn", "virtual_machine", "ephemeral_docker_container", "outbound_vpn"],
        "cleanup_scope": ["container", "temporary_workdir"],
        "deployment_status": "not_deployed",
    }
}

# Model/service bindings and security profiles can be combined independently.
SECTOR_SECURITY_PROFILES = {"FINANCE": "vpn-vm-ephemeral-container-vpn"}


def selected_profile(sector):
    identifier = SECTOR_SECURITY_PROFILES.get(sector)
    return deepcopy(SECURITY_PROFILES[identifier]) if identifier else None


def profile_text(sector, language):
    if selected_profile(sector) is None:
        return None
    if language == "ru":
        return ("Профиль финансов\nВходной VPN → VM → временный Docker-контейнер с очисткой → выходной VPN\n"
                "Схема задана; развёртывание будет подключено отдельно.")
    return ("Finance profile\nInbound VPN → VM → temporary Docker container with cleanup → outbound VPN\n"
            "Profile defined; deployment will be connected separately.")


# Execution routing is intentionally separate from the normal GENESIS API traffic.
# A declared profile is never evidence that any VM, tunnel, or container is running.
ALLOWED_TEST_TOOLS = frozenset({"fixture_lookup", "ui_smoke_test"})


def plan_execution(sector: str, tool: str) -> dict:
    """Resolve an approved test operation without starting or forwarding it."""
    profile = selected_profile(sector)
    if tool not in ALLOWED_TEST_TOOLS:
        return {"status": "rejected", "reason": "tool_not_allowed", "execution_allowed": False}
    if profile is None:
        return {"status": "rejected", "reason": "no_security_profile", "execution_allowed": False}
    if profile["deployment_status"] != "deployed":
        return {
            "status": "blocked",
            "reason": "execution_environment_not_deployed",
            "profile_id": profile["id"],
            "stages": profile["stages"],
            "execution_allowed": False,
        }
    # Fail closed until an authenticated, attested worker dispatch exists.
    return {
        "status": "blocked",
        "reason": "worker_dispatch_not_configured",
        "profile_id": profile["id"],
        "execution_allowed": False,
    }
