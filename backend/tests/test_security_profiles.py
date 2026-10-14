from app.genesis_security_profiles import selected_profile, profile_text


def test_finance_profile_is_not_inherited_by_other_sectors():
    finance = selected_profile("FINANCE")
    assert finance["stages"] == ["inbound_vpn", "virtual_machine", "ephemeral_docker_container", "outbound_vpn"]
    assert finance["deployment_status"] == "not_deployed"
    assert finance["applies_to"] == "sector_services"
    for sector in ("MEDICINE", "MANUFACTURING", "MILITARY", "GOVERNMENT", "LOGISTICS"):
        assert selected_profile(sector) is None
        assert profile_text(sector, "ru") is None


def test_profile_result_does_not_mutate_registry():
    result = selected_profile("FINANCE")
    result["stages"].clear()
    assert len(selected_profile("FINANCE")["stages"]) == 4
