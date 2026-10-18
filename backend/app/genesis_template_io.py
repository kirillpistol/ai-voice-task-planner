"""Transferable GENESIS dataset template files (CSV headers + JSON metadata)."""
import csv
import io
import json
from pathlib import Path

from app.genesis_dataset_store import DatasetStore


def template_csv(store: DatasetStore, template_id: str) -> str:
    definition = store.export_template(template_id)
    buffer = io.StringIO(newline="")
    csv.writer(buffer).writerow(definition["columns"])
    return buffer.getvalue()


def export_template_files(store: DatasetStore, template_id: str, target_dir: str | Path) -> dict:
    """Write a blank import CSV and machine-readable versioned template definition."""
    definition = store.export_template(template_id)
    output = Path(target_dir)
    output.mkdir(parents=True, exist_ok=True)
    stem = f"{definition['sector']}_{definition['name']}_v{definition['revision']}"
    # Avoid using names as filesystem paths if untrusted.
    if not all(c.isalnum() or c in "_-" for c in stem):
        raise ValueError("unsafe_export_name")
    csv_path = output / (stem + ".csv")
    json_path = output / (stem + ".template.json")
    csv_path.write_text(template_csv(store, template_id), encoding="utf-8-sig", newline="")
    json_path.write_text(json.dumps(definition, ensure_ascii=False, indent=2), encoding="utf-8")
    return {"csv": str(csv_path), "template_json": str(json_path)}
