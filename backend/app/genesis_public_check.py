"""Safe public site status checks for GENESIS Level 3."""
import hashlib
import json
import os
from datetime import datetime, timezone
from pathlib import Path
from urllib.request import Request, urlopen

SOURCES = {
    "FINANCE": "https://www.example.com/",
}
def check_public_demo(sector="FINANCE"):
    if sector not in SOURCES:
        raise ValueError("source_not_supported")
    url = SOURCES[sector]
    with urlopen(Request(url, headers={"User-Agent": "GENESIS-public-status/1.0"}), timeout=8) as response:
        data = response.read(65537)
        if response.status != 200 or len(data) > 65536:
            raise ValueError("public_response_unavailable")
    result = {"sector": sector, "source_url": url, "resource": "server HTTPS check",
              "verified_at": datetime.now(timezone.utc).isoformat(),
              "status": "verified_http_200", "bytes": len(data),
              "sha256": hashlib.sha256(data).hexdigest(),
              "content_analyzed": False, "external_app_executed": False}
    report_dir = Path(os.getenv("GENESIS_PUBLIC_REPORT_DIR", "genesis_public_reports"))
    report_dir.mkdir(parents=True, exist_ok=True)
    (report_dir / (sector + ".json")).write_text(json.dumps(result, indent=2), encoding="utf-8")
    return result


def check_and_store_demo(session_factory, sector="FINANCE"):
    """Execute an allowlisted check and persist the verified receipt in SQL."""
    from datetime import datetime
    from app.genesis_store import GenesisSourceObservation
    result = check_public_demo(sector)
    with session_factory() as session:
        row = session.get(GenesisSourceObservation, sector)
        if row is None:
            row = GenesisSourceObservation(sector=sector)
            session.add(row)
        row.source_url = result["source_url"]
        row.resource = result["resource"]
        row.status = result["status"]
        row.checked_at = datetime.fromisoformat(result["verified_at"])
        row.digest = result["sha256"]
        row.result_json = json.dumps(result, ensure_ascii=False)
        session.commit()
    return result
