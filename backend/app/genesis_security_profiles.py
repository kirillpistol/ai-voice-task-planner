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
    return "VPN → VM → DOC → VPN"
