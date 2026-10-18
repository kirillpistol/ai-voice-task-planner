import asyncio
import logging
import os
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from app.v2 import router as v2_router
from app.genesis_api import router as genesis_router
from app.genesis_ai import router as genesis_ai_router
from app.genesis_modules import router as genesis_modules_router
from app.genesis_plans import router as genesis_plans_router
from app.genesis_memory import router as memory_router
from app.genesis_core_status import router as core_status_router, monitor as core_monitor
from app.genesis_store import make_session_factory, configured_database_url
from app.genesis_retention import prune_old_analytical_tasks
from google.cloud import firestore
from google.auth.exceptions import DefaultCredentialsError
from sqlalchemy.exc import SQLAlchemyError

logger = logging.getLogger("genesis.api")
logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO").upper())


async def quarterly_retention_loop(app: FastAPI):
    """Periodic UTC-quarter cleanup; each iteration is safe to repeat."""
    while True:
        factory = getattr(app.state, "genesis_sql_factory", None)
        if factory is not None:
            try:
                def cleanup():
                    with factory() as session:
                        return prune_old_analytical_tasks(session)
                removed = await asyncio.to_thread(cleanup)
                if removed:
                    logger.info("Expired %d GENESIS analytical task copies", removed)
            except Exception:
                logger.exception("GENESIS quarterly cleanup failed")
        await asyncio.sleep(3600)


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
    core_monitor.start()
    retention_job = asyncio.create_task(quarterly_retention_loop(app))
    try:
        yield
    finally:
        retention_job.cancel()
        try:
            await retention_job
        except asyncio.CancelledError:
            pass
        await asyncio.to_thread(core_monitor.stop)


app = FastAPI(title="PISTOL GENESIS API", version="0.2.0", lifespan=lifespan, docs_url=None, redoc_url=None)
app.include_router(v2_router)
app.include_router(genesis_router)
app.include_router(genesis_ai_router)
app.include_router(genesis_modules_router)
app.include_router(genesis_plans_router)
app.include_router(memory_router)
app.include_router(core_status_router)


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

