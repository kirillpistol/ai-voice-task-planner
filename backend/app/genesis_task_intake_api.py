"""Standalone GENESIS task intake service, separate from the universal Core.

Local pilot: accept a CSV batch or generate synthetic case IDs, validate it,
persist it, then return the normalized list for eventual worker dispatch.
"""
from __future__ import annotations

import csv
import io
import os
from pathlib import Path

from fastapi import Depends, FastAPI, Header, HTTPException
from pydantic import BaseModel, Field

from app.genesis_dataset_store import DatasetStore
from app.genesis_template_io import template_csv

app = FastAPI(title="GENESIS Task Intake", version="0.1.0")
SCENARIOS = frozenset({"positive", "negative", "unknown"})
TEMPLATE = ("record_id", "scenario")


def authenticate(x_genesis_token: str | None = Header(default=None)) -> None:
    import hmac
    secret = os.environ.get("GENESIS_TASK_TOKEN", "")
    if not secret:
        raise HTTPException(503, "GENESIS_TASK_TOKEN must be configured")
    if not x_genesis_token or not hmac.compare_digest(secret, x_genesis_token):
        raise HTTPException(401, "Invalid task token")


def store() -> DatasetStore:
    location = Path(os.environ.get("GENESIS_TASK_DB", "genesis_task_data.sqlite"))
    location.parent.mkdir(parents=True, exist_ok=True)
    return DatasetStore(location)


class GenerateRequest(BaseModel):
    count: int = Field(ge=1, le=40)
    prefix: str = Field(default="demo", pattern=r"^demo$")


class UploadRequest(BaseModel):
    csv_text: str = Field(min_length=1, max_length=20000)


def validated_csv(data: str) -> str:
    reader = csv.DictReader(io.StringIO(data))
    if reader.fieldnames != list(TEMPLATE):
        raise HTTPException(422, "Expected columns: record_id,scenario")
    rows = []
    seen = set()
    for row in reader:
        if len(rows) >= 40:
            raise HTTPException(422, "Maximum 40 records per batch")
        if None in row or None in row.values():
            raise HTTPException(422, "Malformed CSV")
        key, scenario = row["record_id"], row["scenario"]
        if not key.startswith("demo-") or len(key) != 8 or not key[5:].isdigit():
            raise HTTPException(422, "Synthetic demo-NNN IDs required")
        if key in seen or scenario not in SCENARIOS:
            raise HTTPException(422, "Duplicate ID or unknown scenario")
        seen.add(key)
        rows.append((key, scenario))
    if not rows:
        raise HTTPException(422, "Empty batch")
    out = io.StringIO(newline="")
    writer = csv.writer(out, lineterminator="\n")
    writer.writerow(TEMPLATE)
    writer.writerows(rows)
    return out.getvalue()


def ingest(csv_text: str, source: str):
    normalized = validated_csv(csv_text)
    db = store()
    # Each batch has its own immutable template and dataset identity.
    template = db.register_template("ANDROID_TEST", "task_queue", list(TEMPLATE))
    imported = db.import_csv(template["template_id"], normalized, max_records=40)
    return {"source": source, "validation": "passed", "status": "ready_for_execution",
            "dataset_id": imported["dataset_id"], "count": imported["count"],
            "tasks": db.compare(imported["dataset_id"])}


@app.get("/healthz")
def healthz():
    return {"service": "genesis-task-intake", "status": "ok"}


@app.get("/v1/template", dependencies=[Depends(authenticate)])
def get_template():
    return {"columns": list(TEMPLATE), "csv_header": "record_id,scenario",
            "scenarios": sorted(SCENARIOS)}


@app.post("/v1/tasks/generate", dependencies=[Depends(authenticate)])
def generate(body: GenerateRequest):
    out = io.StringIO(newline="")
    writer = csv.writer(out, lineterminator="\n")
    writer.writerow(TEMPLATE)
    for idx in range(1, body.count + 1):
        writer.writerow([f"demo-{idx:03d}", "unknown"])
    return ingest(out.getvalue(), "generated")


@app.post("/v1/tasks/import", dependencies=[Depends(authenticate)])
def upload(body: UploadRequest):
    return ingest(body.csv_text, "uploaded")


@app.get("/v1/tasks/{dataset_id}", dependencies=[Depends(authenticate)])
def get_tasks(dataset_id: str):
    if len(dataset_id) != 32 or any(x not in "0123456789abcdef" for x in dataset_id):
        raise HTTPException(422, "Invalid dataset ID")
    db = store()
    with db.connect() as con:
        exists = con.execute("SELECT 1 FROM datasets WHERE dataset_id=?", (dataset_id,)).fetchone()
    if not exists:
        raise HTTPException(404, "Dataset not found")
    return {"dataset_id": dataset_id, "tasks": db.compare(dataset_id)}
