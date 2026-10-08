"""PISTOL GENESIS API bootstrap.

This intentionally does not implement the legacy /api/v1/auth or /api/v1/tasks
contracts until the Kotlin request/response models have been verified.
"""
import logging
import os
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from app.v2 import router as v2_router
from google.cloud import firestore
from google.auth.exceptions import DefaultCredentialsError

logger = logging.getLogger("genesis.api")
logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO").upper())


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.db = None
    try:
        app.state.db = firestore.Client(project=os.getenv("GOOGLE_CLOUD_PROJECT") or None)
        logger.info("Firestore client initialized")
    except (DefaultCredentialsError, ValueError) as exc:
        logger.warning("Firestore credentials not available: %s", exc)
    yield


app = FastAPI(title="PISTOL GENESIS API", version="0.2.0", lifespan=lifespan, docs_url=None, redoc_url=None)
app.include_router(v2_router)


@app.get("/healthz")
def healthz():
    """Liveness; never queries external dependencies."""
    return {"status": "ok", "service": "genesis-api"}


@app.get("/readyz")
def readyz():
    """Readiness requires an authenticated Firestore connection."""
    if app.state.db is None:
        raise HTTPException(status_code=503, detail="Firestore client not initialized")
    try:
        # An RPC to validate credentials and database connectivity.
        list(app.state.db.collections(page_size=1))
    except Exception:
        logger.exception("Firestore readiness check failed")
        raise HTTPException(status_code=503, detail="Firestore unavailable")
    return {"status": "ready"}


@app.get("/api/v1")
def api_info():
    return {"service": "genesis-api", "status": "bootstrap", "contract": "pending verification"}
