"""Read-only X.509 chain, usage and configuration validation for GENESIS test Circle.

No private key is loaded. No Docker actions. A successful result is not proof of
running mTLS connectivity and not permission to process personal data.
"""
import argparse
import hashlib
import json
import ssl
import subprocess
import sys
from pathlib import Path

SERVICES = ("core-a", "core-b", "source-a", "source-b")
EXPECTED = {
    "core-a": ("core-a", "8443"),
    "core-b": ("core-b", "8443"),
    "source-a": ("source-a", "9443"),
    "source-b": ("source-b", "9443"),
}


def command(*args):
    result = subprocess.run(args, capture_output=True, text=True, timeout=10)
    if result.returncode:
        raise ValueError("certificate verification failed")
    return result.stdout


def validate(state: Path) -> dict:
    root = state.resolve()
    entries = []
    errors = []
    checker = root / "checker"
    for name in SERVICES:
        cert = root / name / (name + ".crt")
        ca = root / name / "ca.crt"
        status = "valid"
        try:
            command("openssl", "verify", "-CAfile", str(ca), "-purpose", "sslserver", str(cert))
            # Check the exact DNS identity and server authentication purpose.
            decoded = ssl._ssl._test_decode_cert(str(cert))
            sans = {item[1] for item in decoded.get("subjectAltName", ()) if item[0] == "DNS"}
            if sans != {EXPECTED[name][0]}:
                raise ValueError("unexpected subject alternative name")
            command("openssl", "x509", "-in", str(cert), "-checkend", "86400", "-noout")
            if ca.read_bytes() != (checker / "ca.crt").read_bytes():
                raise ValueError("inconsistent trust root")
            if name.startswith("core-"):
                config = json.loads((root / name / "server.json").read_text())
                expected_roles = {"control", "data"}
                roles = set(role for role_list in config["client_roles"].values() for role in role_list)
                if roles != expected_roles:
                    raise ValueError("unexpected client roles")
                policy = json.loads((root / name / "policy.json").read_text())
                if not policy.get("bindings") or not policy.get("data_grants"):
                    raise ValueError("missing binding policy")
            else:
                config = json.loads((root / name / "server.json").read_text())
                if len(config.get("client_pins", [])) != 1:
                    raise ValueError("invalid source client pins")
            # Pins are sha256 of the DER-encoded leaf, not of PEM text.
            pem = cert.read_text()
            pin = hashlib.sha256(ssl.PEM_cert_to_DER_cert(pem)).hexdigest()
            for client in ("control", "data", "outsider"):
                cfg = json.loads((checker / (client + ".json")).read_text())
                if name.startswith("core-") and pin not in cfg["peers"].get("https://" + name + ":8443", []):
                    raise ValueError("checker peer pin mismatch")
            if name.startswith("source-"):
                cfg = json.loads((checker / "parts.json").read_text())
                if pin not in cfg["peers"].get("https://" + name + ":9443", []):
                    raise ValueError("source peer pin mismatch")
        except (OSError, ValueError, KeyError, TypeError, json.JSONDecodeError, subprocess.TimeoutExpired) as exc:
            status = "invalid"
            # Never include exception text: it may include internal paths or metadata.
            errors.append(name)
        entries.append({"component": name, "status": status})
    for role in ("control", "data"):
        try:
            command("openssl", "verify", "-CAfile", str(checker / "ca.crt"),
                    "-purpose", "sslclient", str(checker / (role + ".crt")))
            command("openssl", "x509", "-in", str(checker / (role + ".crt")),
                    "-checkend", "86400", "-noout")
            entries.append({"component": "checker/" + role, "status": "valid"})
        except (OSError, ValueError, subprocess.TimeoutExpired):
            errors.append("checker/" + role)
            entries.append({"component": "checker/" + role, "status": "invalid"})
    return {"status": "passed" if not errors else "failed",
            "checks": entries, "failed_components": errors,
            "read_only": True, "rotation_enabled": False}


def main() -> int:
    parser = argparse.ArgumentParser(description="Offline GENESIS Circle trust validation")
    parser.add_argument("--circle", type=Path, required=True)
    args = parser.parse_args()
    result = validate(args.circle / "state")
    print(json.dumps(result, indent=2))
    return 0 if result["status"] == "passed" else 2


if __name__ == "__main__":
    sys.exit(main())
