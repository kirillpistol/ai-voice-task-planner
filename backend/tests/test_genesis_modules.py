from fastapi import HTTPException
import pytest

from app.genesis_modules import CATALOG, ModuleRequest, module_catalog, request_module_job


def test_catalog_declares_no_live_l3_execution():
    entries = module_catalog(uid="fixture-user")
    assert {entry.module_id for entry in entries} == {"mesm", "med"}
    assert all(entry.deployment_status == "not_deployed" for entry in entries)


def test_module_jobs_fail_closed_without_consent():
    with pytest.raises(HTTPException) as error:
        request_module_job(ModuleRequest(module_id="mesm", consent_to_module_processing=False), uid="fixture")
    assert error.value.status_code == 400


def test_module_jobs_fail_closed_even_with_consent():
    with pytest.raises(HTTPException) as error:
        request_module_job(ModuleRequest(module_id="med", consent_to_module_processing=True), uid="fixture")
    assert error.value.status_code == 503
