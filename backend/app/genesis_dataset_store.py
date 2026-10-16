"""Versioned dataset storage for GENESIS sector services.

SQLite reference implementation. Sector services can replace persistence behind
this interface without changing GENESIS Core. Source records here are test IDs.
"""
from __future__ import annotations
import csv
import hashlib
import io
import json
import sqlite3
from datetime import datetime, timezone
from pathlib import Path
from uuid import uuid4


def now() -> str:
    return datetime.now(timezone.utc).isoformat()


SCHEMA = """
PRAGMA foreign_keys=ON;
CREATE TABLE IF NOT EXISTS templates (
  template_id TEXT PRIMARY KEY,
  sector TEXT NOT NULL,
  name TEXT NOT NULL,
  revision INTEGER NOT NULL,
  schema_json TEXT NOT NULL,
  created_at TEXT NOT NULL,
  UNIQUE(sector, name, revision)
);
CREATE TABLE IF NOT EXISTS datasets (
  dataset_id TEXT PRIMARY KEY,
  template_id TEXT NOT NULL REFERENCES templates(template_id),
  source_sha256 TEXT NOT NULL,
  created_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS records (
  dataset_id TEXT NOT NULL REFERENCES datasets(dataset_id),
  record_id TEXT NOT NULL,
  source_json TEXT NOT NULL,
  PRIMARY KEY(dataset_id, record_id)
);
CREATE TABLE IF NOT EXISTS observations (
  observation_id TEXT PRIMARY KEY,
  dataset_id TEXT NOT NULL,
  record_id TEXT NOT NULL,
  app_adapter TEXT NOT NULL,
  adapter_version TEXT NOT NULL,
  result_json TEXT NOT NULL,
  observed_at TEXT NOT NULL,
  FOREIGN KEY(dataset_id,record_id) REFERENCES records(dataset_id,record_id)
);
CREATE TABLE IF NOT EXISTS ui_revisions (
  revision_id TEXT PRIMARY KEY,
  app_adapter TEXT NOT NULL,
  fingerprint TEXT NOT NULL,
  selectors_json TEXT NOT NULL,
  observed_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS adaptation_proposals (
  proposal_id TEXT PRIMARY KEY,
  app_adapter TEXT NOT NULL,
  old_revision_id TEXT,
  new_revision_id TEXT NOT NULL REFERENCES ui_revisions(revision_id),
  proposal_json TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending_review',
  created_at TEXT NOT NULL
);
"""


class DatasetStore:
    def __init__(self, path: str | Path):
        self.path = str(path)
        with self.connect() as db:
            db.executescript(SCHEMA)

    def connect(self):
        db = sqlite3.connect(self.path)
        db.row_factory = sqlite3.Row
        db.execute("PRAGMA foreign_keys=ON")
        return db

    def register_template(self, sector: str, name: str, columns: list[str]) -> dict:
        if not sector or not name or not columns or len(set(columns)) != len(columns):
            raise ValueError("invalid_template")
        if "record_id" not in columns:
            raise ValueError("record_id_column_required")
        if len(columns) > 50 or any(not c.isidentifier() or len(c) > 64 for c in columns):
            raise ValueError("invalid_columns")
        with self.connect() as db:
            rev = db.execute(
                "SELECT COALESCE(MAX(revision),0)+1 FROM templates WHERE sector=? AND name=?",
                (sector, name),
            ).fetchone()[0]
            template_id = uuid4().hex
            db.execute(
                "INSERT INTO templates VALUES (?,?,?,?,?,?)",
                (template_id, sector, name, rev, json.dumps(columns), now()),
            )
        return {"template_id": template_id, "sector": sector, "name": name,
                "revision": rev, "columns": columns}

    def export_template(self, template_id: str) -> dict:
        with self.connect() as db:
            row = db.execute("SELECT * FROM templates WHERE template_id=?", (template_id,)).fetchone()
            if row is None:
                raise KeyError("template_not_found")
            return {"template_id": row["template_id"], "sector": row["sector"],
                    "name": row["name"], "revision": row["revision"],
                    "columns": json.loads(row["schema_json"])}

    def import_csv(self, template_id: str, csv_text: str, max_records: int = 10000) -> dict:
        template = self.export_template(template_id)
        if len(csv_text.encode("utf-8")) > 2_000_000:
            raise ValueError("source_too_large")
        reader = csv.DictReader(io.StringIO(csv_text))
        if reader.fieldnames != template["columns"]:
            raise ValueError("csv_header_does_not_match_template")
        records = []
        seen = set()
        for record in reader:
            if len(records) >= max_records:
                raise ValueError("record_limit_exceeded")
            if None in record or any(v is None for v in record.values()):
                raise ValueError("malformed_csv")
            record_id = record["record_id"]
            if not record_id or record_id in seen:
                raise ValueError("duplicate_or_empty_record_id")
            seen.add(record_id)
            records.append((record_id, json.dumps(record, ensure_ascii=False)))
        dataset_id = uuid4().hex
        digest = hashlib.sha256(csv_text.encode("utf-8")).hexdigest()
        with self.connect() as db:
            db.execute("INSERT INTO datasets VALUES (?,?,?,?)",
                       (dataset_id, template_id, digest, now()))
            db.executemany("INSERT INTO records VALUES (?,?,?)",
                           [(dataset_id, record_id, raw) for record_id, raw in records])
        return {"dataset_id": dataset_id, "template_id": template_id,
                "source_sha256": digest, "count": len(records)}

    def record_observation(self, dataset_id: str, record_id: str, app_adapter: str,
                           adapter_version: str, result: dict) -> str:
        observation_id = uuid4().hex
        with self.connect() as db:
            db.execute("INSERT INTO observations VALUES (?,?,?,?,?,?,?)",
                       (observation_id, dataset_id, record_id, app_adapter,
                        adapter_version, json.dumps(result, ensure_ascii=False), now()))
        return observation_id

    def compare(self, dataset_id: str) -> list[dict]:
        """Left-join all source records to their latest observed result."""
        with self.connect() as db:
            rows = db.execute("""
                SELECT r.record_id, r.source_json, o.result_json, o.observed_at
                FROM records r LEFT JOIN observations o ON o.observation_id=(
                  SELECT observation_id FROM observations
                  WHERE dataset_id=r.dataset_id AND record_id=r.record_id
                  ORDER BY observed_at DESC, rowid DESC LIMIT 1
                ) WHERE r.dataset_id=? ORDER BY r.record_id
            """, (dataset_id,)).fetchall()
        return [
            {"record_id": row["record_id"], "source": json.loads(row["source_json"]),
             "result": json.loads(row["result_json"]) if row["result_json"] else None,
             "status": "observed" if row["result_json"] else "pending",
             "observed_at": row["observed_at"]}
            for row in rows
        ]

    def add_ui_revision(self, app_adapter: str, selectors: dict) -> dict:
        canonical = json.dumps(selectors, sort_keys=True, ensure_ascii=False)
        fingerprint = hashlib.sha256(canonical.encode()).hexdigest()
        revision_id = uuid4().hex
        with self.connect() as db:
            db.execute("INSERT INTO ui_revisions VALUES (?,?,?,?,?)",
                       (revision_id, app_adapter, fingerprint, canonical, now()))
        return {"revision_id": revision_id, "fingerprint": fingerprint}

    def propose_adaptation(self, app_adapter: str, old_revision_id: str | None,
                           new_revision_id: str, changes: dict) -> str:
        proposal_id = uuid4().hex
        with self.connect() as db:
            db.execute("INSERT INTO adaptation_proposals VALUES (?,?,?,?,?,?,?)",
                       (proposal_id, app_adapter, old_revision_id, new_revision_id,
                        json.dumps(changes, ensure_ascii=False), "pending_review", now()))
        return proposal_id
