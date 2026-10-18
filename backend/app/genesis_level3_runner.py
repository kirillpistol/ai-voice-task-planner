"""Server-only GENESIS Level 3 worker entry point.

Run by an OS scheduler or manually; Android is strictly read-only.
Only the hardcoded public demonstration source is checked.
"""
import argparse
import json
import logging
import sys
from pathlib import Path

from app.genesis_public_check import check_and_store_demo
from app.genesis_store import configured_database_url, make_session_factory

logger = logging.getLogger("genesis.level3")


def execute_once() -> dict:
    url = configured_database_url()
    if not url:
        raise RuntimeError("GENESIS database is not configured")
    factory = make_session_factory(url)
    result = check_and_store_demo(factory, "FINANCE")
    return {
        "status": result["status"], "sector": result["sector"],
        "verified_at": result["verified_at"],
        "source_url": result["source_url"],
        "sha256": result["sha256"],
        "content_analyzed": False,
        "external_app_executed": False,
    }


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description="Run one GENESIS Level 3 public check")
    parser.add_argument("--once", action="store_true", required=True)
    args = parser.parse_args(argv)
    try:
        print(json.dumps(execute_once(), ensure_ascii=False))
        return 0
    except Exception as exc:
        logger.exception("Level 3 check failed")
        print(json.dumps({"status": "failed", "error": type(exc).__name__}))
        return 1


if __name__ == "__main__":
    sys.exit(main())
