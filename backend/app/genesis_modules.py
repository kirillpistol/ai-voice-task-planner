"""L2 module registry: declared capabilities only. Never executes remote code.

Ordinary comment/OpenAI traffic bypasses CORE. Specialized modules require a
separate, explicit opt-in and attested L3 infrastructure before dispatch.
"""
from typing import Literal

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field

from app.v2 import current_uid

router = APIRouter(prefix="/api/v2/genesis/modules", tags=["genesis-modules"])


class ModuleManifest(BaseModel):
    module_id: Literal["mesm", "med"]
    execution_mode: Literal["dedicated_l3"] = "dedicated_l3"
    output_format: Literal["pdf", "json"] = "pdf"
    permissions: list[str] = Field(default_factory=list)
    deployment_status: Literal["not_deployed"] = "not_deployed"


CATALOG = {
    "mesm": ModuleManifest(module_id="mesm", permissions=["approved_economic_data"]),
    "med": ModuleManifest(module_id="med", permissions=["consented_medical_documents"]),
}


@router.get("/catalog", response_model=list[ModuleManifest])
def module_catalog(uid: str = Depends(current_uid)):
    """Read-only catalogue; no user module is activated by listing it."""
    return list(CATALOG.values())


class ModuleRequest(BaseModel):
    module_id: Literal["mesm", "med"]
    consent_to_module_processing: bool


@router.post("/jobs")
def request_module_job(request: ModuleRequest, uid: str = Depends(current_uid)):
    """Fail-closed until per-user L2 storage, permissions and L3 dispatch are deployed."""
    if not request.consent_to_module_processing:
        raise HTTPException(status_code=400, detail="Explicit module consent required")
    raise HTTPException(status_code=503, detail="Dedicated L3 dispatch is not deployed")
