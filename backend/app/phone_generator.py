"""Random phone-number generation for development and test data.

Numbers are syntactically formatted only. They may belong to real subscribers.
Do not call, message, or treat them as available/verified phone numbers.
"""
import secrets

from fastapi import APIRouter, Query

router = APIRouter(prefix="/api/v1/tools", tags=["tools"])


@router.get("/generate-phone")
def generate_phone(
    prefix: str = Query(default="999", pattern=r"^9[0-9]{2}$"),
    count: int = Query(default=1, ge=1, le=1000),
):
    """Generate unique, unverified +7-prefixed numbers within this response."""
    numbers: set[str] = set()
    while len(numbers) < count:
        suffix = f"{secrets.randbelow(10_000_000):07d}"
        numbers.add(f"+7{prefix}{suffix}")
    return {"count": len(numbers), "phones": sorted(numbers)}
