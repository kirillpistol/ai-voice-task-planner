"""Read-only metadata for sector verification sources and execution resources.

Configured metadata is not proof of a completed web check or worker invocation.
No calls to an external application are made here.
"""
import json
import os
from pathlib import Path
from urllib.parse import urlparse


def sector_source_status(sector: str, language: str = "ru") -> dict:
    raw = os.getenv("GENESIS_SECTOR_SOURCES_JSON", "").strip()
    configured = {}
    if raw:
        try:
            parsed = json.loads(raw)
            if isinstance(parsed, dict):
                configured = parsed
        except (ValueError, TypeError):
            pass
    item = configured.get(sector, {})
    if not isinstance(item, dict):
        item = {}
    url = str(item.get("source_url") or "").strip()
    parsed_url = urlparse(url)
    valid_url = (parsed_url.scheme == "https" and bool(parsed_url.hostname)
                 and not parsed_url.username and not parsed_url.password)
    source_name = str(item.get("source_name") or "").strip()[:120]
    resource_name = str(item.get("resource_name") or "").strip()[:120]
    resource_type = str(item.get("resource_type") or "").strip().lower()
    if resource_type not in ("web", "api", "android_app", "service"):
        resource_type = ""
    # Completed public checks are read-only receipts produced by Level 3.
    # They never imply that an app, VPN or virtual machine was started.
    receipt = None
    report_path = Path(os.getenv("GENESIS_PUBLIC_REPORT_DIR", "genesis_public_reports")) / (sector + ".json")
    if sector in ("FINANCE", "MEDICINE", "MANUFACTURING", "MILITARY", "GOVERNMENT", "LOGISTICS") and report_path.is_file():
        try:
            candidate = json.loads(report_path.read_text(encoding="utf-8"))
            if candidate.get("source_url") == url and candidate.get("status") == "verified_http_200":
                receipt = candidate
        except (ValueError, OSError):
            pass
    ru = language == "ru"
    source = (
        f"{source_name} — {url}\n" +
        ("Настроен; проверка ещё не выполнялась" if ru else "Configured; not yet verified")
    ) if valid_url and source_name else (
        "Источник проверки не подключён" if ru else "Verification source not connected"
    )
    resource = (
        f"{resource_name} ({resource_type})\n" +
        ("Назначен; запуск исполнителя не подтверждён" if ru
         else "Assigned; worker execution not confirmed")
    ) if resource_name and resource_type else (
        "Ресурс не подключён" if ru else "Execution resource not connected"
    )
    if receipt:
        note = "HTTP 200 проверен" if ru else "HTTP 200 verified"
        checked_at = receipt.get("verified_at", "unknown")
        source = f"{source_name} — {url}\n{note}: {checked_at}"
        resource = ("Серверная HTTPS-проверка" if ru else "Server HTTPS check") + "\n" + receipt.get("sha256", "")[:16]
    return {
        "source": source,
        "connection_resource": resource,
        "source_configured": bool(valid_url and source_name),
        "resource_configured": bool(resource_name and resource_type),
        "verification_completed": bool(receipt),
        "worker_executed": bool(receipt),
    }
