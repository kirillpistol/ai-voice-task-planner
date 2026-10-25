"""Read-only certificate expiry inventory for a synthetic GENESIS Circle.

Never opens private keys, issues certificates, restarts services or changes Docker.
This is monitoring scaffolding, NOT automatic rotation.
"""
import argparse
import json
import ssl
import sys
from datetime import datetime, timezone
from pathlib import Path

CERTIFICATES = (
    "core-a/core-a.crt",
    "core-b/core-b.crt",
    "source-a/source-a.crt",
    "source-b/source-b.crt",
    "checker/ca.crt",
    "checker/data.crt",
    "checker/control.crt",
)


def inspect_circle(circle: Path, warning_days: int = 7, now: datetime | None = None) -> dict:
    if warning_days < 0:
        raise ValueError("warning_days must be nonnegative")
    now = now or datetime.now(timezone.utc)
    state = circle.resolve() / "state"
    entries = []
    for name in CERTIFICATES:
        path = state / name
        try:
            # Decode only X.509 public certificate metadata.
            certificate = ssl._ssl._test_decode_cert(str(path))
            expires = datetime.strptime(certificate["notAfter"], "%b %d %H:%M:%S %Y %Z").replace(tzinfo=timezone.utc)
            days_left = (expires - now).total_seconds() / 86400
            status = "expired" if days_left <= 0 else ("expiring" if days_left <= warning_days else "valid")
            entries.append({"certificate": name, "expires_utc": expires.isoformat(),
                            "days_remaining": round(days_left, 2), "status": status})
        except (OSError, ValueError, KeyError, ssl.SSLError):
            entries.append({"certificate": name, "status": "unreadable"})
    states = {entry["status"] for entry in entries}
    overall = ("critical" if states & {"expired", "unreadable"} else
               "warning" if "expiring" in states else "ok")
    return {"overall": overall, "checked_at_utc": now.isoformat(),
            "warning_days": warning_days, "certificates": entries,
            "rotation_enabled": False}


def main() -> int:
    parser = argparse.ArgumentParser(description="Read-only GENESIS test-circle certificate inventory")
    parser.add_argument("--circle", type=Path, required=True, help="Circle workspace (no secrets are printed)")
    parser.add_argument("--warn-days", type=int, default=7)
    args = parser.parse_args()
    try:
        report = inspect_circle(args.circle, args.warn_days)
    except ValueError as error:
        parser.error(str(error))
    print(json.dumps(report, indent=2))
    return {"ok": 0, "warning": 1, "critical": 2}[report["overall"]]


if __name__ == "__main__":
    sys.exit(main())
