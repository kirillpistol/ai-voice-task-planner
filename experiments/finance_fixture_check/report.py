"""L3 report preparation without exporting labels, phones or credentials."""
from collections import Counter
from datetime import datetime, timezone
import json
from pathlib import Path


def report(results):
    counts = Counter(result.verdict.value for result in results)
    return {
        "contract": "finance-fixture-check/1",
        "environment": "offline-simulation",
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "counts": {name: counts[name] for name in ("match", "mismatch", "not_found", "error")},
        "results": [{"case_id": row.case_id, "verdict": row.verdict.value, "reason": row.reason} for row in results],
    }


def save(document, path):
    # Exclusive create prevents accidental replacement of a previous report.
    with Path(path).open("x", encoding="utf-8") as file:
        json.dump(document, file, ensure_ascii=False, indent=2)
