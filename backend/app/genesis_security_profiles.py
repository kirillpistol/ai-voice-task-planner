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
    },
    "p2-doc-vpn-doc-p2-medicine": {
        "id": "p2-doc-vpn-doc-p2-medicine",
        "version": 1,
        "applies_to": "medicine_sector_authorized_data_ingestion",
        "stages": ["p2_inbound_gateway", "isolated_document_ingest", "private_vpn_link",
                   "isolated_document_processing", "p2_outbound_gateway"],
        "data_scope": ["consented_documents", "authorized_telemetry"],
        "storage": "private_encrypted_user_scoped_storage",
        "retention": "requires_policy_and_configured_expiry",
        "traffic_capture": "only_explicitly_authorized_sources; no passive third-party interception",
        "deployment_status": "not_deployed",
    }
}

# Model/service bindings and security profiles can be combined independently.
SECTOR_SECURITY_PROFILES = {"FINANCE": "vpn-vm-ephemeral-container-vpn", "MEDICINE": "p2-doc-vpn-doc-p2-medicine"}


def selected_profile(sector):
    identifier = SECTOR_SECURITY_PROFILES.get(sector)
    return deepcopy(SECURITY_PROFILES[identifier]) if identifier else None


def profile_text(sector, language):
    if selected_profile(sector) is None:
        return None
    if sector == "MEDICINE":
        if language == "ru":
            return ("MEDICINE · P2 - DOC - VPN - DOC - P2\\n"
                    "Отдельный серверный модуль: приём разрешённых медицинских документов и телеметрии, "
                    "закрытый канал, изолированная обработка, контроль доступа, журналирование и сроки хранения.\\n"
                    "Сетевой трафик — только от принадлежащих системе либо явно разрешённых источников; "
                    "скрытый перехват чужих данных не предусмотрен.\\n"
                    "Профиль выбран; модуль ещё не развёрнут.")
        return ("MEDICINE · P2 - DOC - VPN - DOC - P2\\n"
                "Separate server module for authorized documents and telemetry, private tunnel, "
                "isolated processing, access controls, audit and retention.\\n"
                "Capture restricted to owned or expressly authorized traffic; no covert interception.\\n"
                "Profile selected; module not deployed.")
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


# Fixed synthetic fixture catalogue, entirely local. Never sends network requests.
# The keys are test identifiers, not telephone numbers.
DEMO_FIXTURES = {
    "demo-001": "Иванов Иван И.",
    "demo-002": None,
    "demo-003": "Петрова Анна С.",
    "demo-004": None,
}


def run_fixture_demo(case_ids: list[str]) -> dict:
    """Execute an in-process test double, not a Level 3 security-profile dispatch."""
    from time import perf_counter

    if len(case_ids) > 40:
        raise ValueError("Batch is limited to 40 test cases")
    outcomes = []
    started = perf_counter()
    for case_id in case_ids:
        tick = perf_counter()
        # Simulate clearing the input before every case.
        input_value = ""
        input_value = case_id
        if input_value not in DEMO_FIXTURES:
            outcomes.append({"case_id": case_id, "result": "error",
                             "duration_ms": round((perf_counter() - tick) * 1000, 3)})
            continue
        name = DEMO_FIXTURES[input_value]
        outcomes.append({"case_id": case_id, "result": "+" if name else "-",
                         "display_name": name,
                         "duration_ms": round((perf_counter() - tick) * 1000, 3)})
    elapsed = perf_counter() - started
    positives = sum(row["result"] == "+" for row in outcomes)
    negatives = sum(row["result"] == "-" for row in outcomes)
    completed = positives + negatives
    return {
        "mode": "local_fixture_simulation",
        "isolated_execution": False,
        "count": len(outcomes),
        "positive": positives,
        "negative": negatives,
        "errors": len(outcomes) - completed,
        "positive_rate": round(positives / completed, 4) if completed else None,
        "elapsed_ms": round(elapsed * 1000, 3),
        "throughput_per_second": round(len(outcomes) / elapsed, 2) if elapsed else None,
        "results": outcomes,
    }
