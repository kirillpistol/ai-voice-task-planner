"""Standalone GENESIS task intake service, separate from the universal Core.

Local pilot: accept a CSV batch or generate synthetic case IDs, validate it,
persist it, then return the normalized list for eventual worker dispatch.
"""
from __future__ import annotations

import csv
import io
import os
import json
from datetime import datetime, timezone
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


@app.get("/v1/security/profile", dependencies=[Depends(authenticate)])
def security_profile():
    from app.genesis_security_profiles import selected_profile, plan_execution
    profile = selected_profile("FINANCE")
    if profile is None:
        raise HTTPException(503, "Level 2 profile not configured")
    routing = plan_execution("FINANCE", "ui_smoke_test")
    return {
        "level": 2,
        "service": "FINANCE",
        "profile_selected": True,
        "profile_id": profile["id"],
        "stages": profile["stages"],
        "deployment_status": profile["deployment_status"],
        "execution_allowed": routing["execution_allowed"],
        "execution_reason": routing.get("reason"),
        "status_note": "Profile configured, isolation not deployed"
                       if profile["deployment_status"] != "deployed"
                       else "Profile configured; verify live worker separately",
    }


@app.get("/v1/tasks/{dataset_id}/review", dependencies=[Depends(authenticate)])
def review_tasks(dataset_id: str):
    data = get_tasks(dataset_id)
    tasks = data["tasks"]
    breakdown = {"positive": 0, "negative": 0, "unknown": 0}
    for task in tasks:
        scenario = task["source"]["scenario"]
        breakdown[scenario] += 1
    recommendations = []
    if breakdown["unknown"]:
        recommendations.append(
            f"{breakdown['unknown']} record(s) have unknown scenario; review labels before execution."
        )
    if not breakdown["unknown"]:
        recommendations.append("All records have a declared scenario.")
    recommendations.append("No external-app result is inferred from imported labels.")
    return {"dataset_id": dataset_id, "analysis_mode": "deterministic_validation",
            "record_count": len(tasks), "scenario_counts": breakdown,
            "recommendations": recommendations,
            "android_execution_completed": False}


class ProcessRequest(BaseModel):
    """Run actual server validation and record observations, without external app access."""
    confirm: bool = True


def report_directory() -> Path:
    folder = Path(os.environ.get("GENESIS_REPORT_DIR", "genesis_reports"))
    folder.mkdir(parents=True, exist_ok=True)
    return folder


@app.post("/v1/tasks/{dataset_id}/process", dependencies=[Depends(authenticate)])
def process_batch(dataset_id: str, body: ProcessRequest):
    if not body.confirm:
        raise HTTPException(422, "Confirm validation run")
    entries = get_tasks(dataset_id)["tasks"]
    db = store()
    for entry in entries:
        # Validation is an actual outcome; imported category labels are never
        # interpreted as verified facts from an external application.
        record = entry["source"]
        result = {
            "stage": "data_validation", "status": "passed",
            "valid_record_id": record["record_id"].startswith("demo-"),
            "category_label": record["scenario"],
            "external_lookup_performed": False,
        }
        db.record_observation(dataset_id, entry["record_id"], "genesis-validator",
                              "1", result)
    comparisons = db.compare(dataset_id)
    reviewed = review_tasks(dataset_id)
    report = {
        "dataset_id": dataset_id, "created_at": datetime.now(timezone.utc).isoformat(),
        "status": "validated", "process": "internal_dataset_validation",
        "records": comparisons, "review": reviewed,
        "security": security_profile(),
        "external_lookup_performed": False, "android_execution_completed": False,
    }
    output = report_directory() / (dataset_id + ".json")
    temp = output.with_suffix(".json.tmp")
    temp.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
    temp.replace(output)
    return {
        "dataset_id": dataset_id, "status": "validated",
        "count": len(entries), "report_file": output.name,
        "recommendations": reviewed["recommendations"],
        "external_lookup_performed": False,
        "android_execution_completed": False,
    }


@app.get("/v1/tasks/{dataset_id}/report", dependencies=[Depends(authenticate)])
def read_report(dataset_id: str):
    get_tasks(dataset_id)  # checks dataset existence and identifier
    path = report_directory() / (dataset_id + ".json")
    if not path.is_file():
        raise HTTPException(404, "Process this dataset first")
    return json.loads(path.read_text(encoding="utf-8"))
