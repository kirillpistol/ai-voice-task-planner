"""Public package catalog and authenticated enterprise inquiries.

Plan changes and payment entitlements are deliberately server-only.
"""
from datetime import datetime, timezone
from uuid import uuid4

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field
from sqlalchemy import select, func
from sqlalchemy.orm import Session

from app.genesis_store import GenesisEnterpriseInquiry, require_sql_session
from app.v2 import current_uid

router = APIRouter(prefix="/api/v2/genesis/plans", tags=["genesis-plans"])


@router.get("")
def list_plans():
    return [
        {"id": "free", "name": "GENESIS Free", "monthly_requests": 100, "price": 0, "status": "available"},
        {"id": "plus", "name": "GENESIS Plus", "monthly_requests": 1000, "price": None, "status": "payment_not_configured"},
        {"id": "dedicated", "name": "PISTOL GENESIS", "monthly_requests": None, "price": None, "status": "inquiry_only"},
    ]


class EnterpriseInquiryRequest(BaseModel):
    comment: str = Field(min_length=10, max_length=3000)


@router.post("/inquiries", status_code=201)
def create_inquiry(body: EnterpriseInquiryRequest,
                   uid: str = Depends(current_uid),
                   session: Session = Depends(require_sql_session)):
    if not body.comment.strip() or len(body.comment.strip()) < 10:
        raise HTTPException(422, "Describe your project in at least 10 characters")
    # Limit repeated submissions per account without exposing other users' data.
    count = session.scalar(select(func.count()).select_from(GenesisEnterpriseInquiry)
                           .where(GenesisEnterpriseInquiry.user_id == uid))
    if count >= 10:
        raise HTTPException(429, "Inquiry limit reached")
    item = GenesisEnterpriseInquiry(
        id=str(uuid4()), user_id=uid, comment=body.comment.strip(),
        created_at=datetime.now(timezone.utc)
    )
    session.add(item)
    session.commit()
    return {"id": item.id, "status": "received"}
