import logging
import os
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from app.v2 import router as v2_router
from app.genesis_api import router as genesis_router
from app.genesis_memory import router as memory_router
from app.genesis_store import make_session_factory, configured_database_url
from google.cloud import firestore
from google.auth.exceptions import DefaultCredentialsError
from sqlalchemy.exc import SQLAlchemyError

logger = logging.getLogger("genesis.api")
logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO").upper())


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.db = None
    app.state.genesis_sql_factory = None
    try:
        app.state.db = firestore.Client(project=os.getenv("GOOGLE_CLOUD_PROJECT") or None)
        logger.info("Firestore client initialized")
    except (DefaultCredentialsError, ValueError) as exc:
        logger.warning("Firestore credentials not available: %s", exc)
    try:
        database_url = configured_database_url()
    except OSError:
        # A missing/unreadable optional secret must not stop Calendar/Firebase.
        logger.exception("GENESIS SQL secret unavailable; Calendar API remains active")
        database_url = ""
    if database_url:
        try:
            app.state.genesis_sql_factory = make_session_factory(database_url)
            logger.info("GENESIS SQL storage initialized")
        except (SQLAlchemyError, OSError):
            # An unavailable optional SQL database must not take Calendar offline.
            app.state.genesis_sql_factory = None
            logger.exception("GENESIS SQL unavailable; Calendar API remains active")
    else:
        logger.warning("GENESIS SQL is not configured (workspace endpoints will return 503)")
    yield


app = FastAPI(title="PISTOL GENESIS API", version="0.2.0", lifespan=lifespan, docs_url=None, redoc_url=None)
app.include_router(v2_router)
app.include_router(genesis_router)
app.include_router(memory_router)


@app.get("/healthz")
def healthz():
    return {"status": "ok", "service": "genesis-api"}


@app.get("/readyz")
def readyz():
    if app.state.db is None:
        raise HTTPException(status_code=503, detail="Firestore client not initialized")
    try:
        next(app.state.db.collections(), None)
    except Exception:
        logger.exception("Firestore readiness check failed")
        raise HTTPException(status_code=503, detail="Firestore unavailable")
    return {"status": "ready"}


@app.get("/api/v1")
def api_info():
    return {"service": "genesis-api", "status": "bootstrap", "contract": "pending verification"}

