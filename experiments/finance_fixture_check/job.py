"""Execute a bounded synthetic job and atomically publish its report locally."""
import argparse
import json
import os
from pathlib import Path
import re
import tempfile

from .models import AndroidSession
from .report import report
from .runner import run
from .simulator import FIXTURES, SimulatedAndroidAdapter


def validate(document):
    if not isinstance(document, dict) or set(document) != {
        "version", "job_id", "environment", "fixture_ids"
    }:
        raise ValueError("Invalid job fields")
    if type(document["version"]) is not int or document["version"] != 1:
        raise ValueError("Unsupported job version")
    if document["environment"] != "offline-simulation":
        raise ValueError("Only synthetic jobs are supported")
    job_id = document["job_id"]
    if not isinstance(job_id, str) or not re.fullmatch(r"[A-Za-z0-9_-]{1,64}", job_id):
        raise ValueError("Invalid job ID")
    ids = document["fixture_ids"]
    known = {item.case_id for item in FIXTURES}
    if (not isinstance(ids, list) or not 1 <= len(ids) <= len(known)
            or any(not isinstance(item, str) or item not in known for item in ids)
            or len(set(ids)) != len(ids)):
        raise ValueError("Unknown or duplicate fixture IDs")
    return job_id, ids


def execute(document, output_directory):
    job_id, ids = validate(document)
    output = Path(output_directory)
    output.mkdir(parents=True, exist_ok=True)
    target = output / (job_id + ".json")
    # Claim the ID before executing. Failed/incomplete jobs retain the claim and
    # require operator review rather than silently repeating an operation.
    claim = output / (job_id + ".claim")
    with claim.open("x", encoding="utf-8") as file:
        file.write("offline-simulation\n")
    if target.exists():
        raise FileExistsError("Report already exists")
    mapping = {item.case_id: item for item in FIXTURES}
    adapter = SimulatedAndroidAdapter()
    session = AndroidSession("genesis-synthetic-fixtures", "synthetic-session", True)
    result = report(run(adapter, session, [mapping[item] for item in ids], set(mapping)))
    result.update(job_id=job_id, status="completed")
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", dir=output,
                                         prefix=".report-", delete=False) as file:
            temporary = Path(file.name)
            json.dump(result, file, ensure_ascii=False, indent=2)
            file.flush()
            os.fsync(file.fileno())
        # Link publishes the complete file without overwriting any existing report.
        os.link(temporary, target)
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)
    return target


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--job", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    try:
        raw = Path(args.job).read_bytes()
        if len(raw) > 16384:
            raise ValueError("Job exceeds size limit")
        target = execute(json.loads(raw), args.output)
    except (ValueError, OSError):
        print(json.dumps({"status": "rejected_or_failed"}))
        return 1
    print(json.dumps({"status": "completed", "report": str(target)}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
