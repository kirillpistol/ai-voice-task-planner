"""Issue separate short-lived identities and approve a synthetic strict binding."""
import argparse
import hashlib
import json
import shutil
import ssl
import subprocess
import tempfile
from pathlib import Path
from urllib.parse import urlsplit

from level1_core.bindings import digest
from level1_core.contracts import dumps
from level2_algorithms.bindings_demo import PACKAGE_SPECS

DEFAULT_URLS = {
    "core-a": "https://core-a:8443", "core-b": "https://core-b:8443",
    "source-a": "https://source-a:9443", "source-b": "https://source-b:9443",
}


def prepare(directory, urls=None, container_paths=True):
    root = Path(directory).resolve()
    root.mkdir(parents=True, exist_ok=True)
    if any(root.iterdir()):
        raise ValueError("Use an empty circle state directory")
    root.chmod(0o700)
    urls = dict(DEFAULT_URLS if urls is None else urls)
    if set(urls) != set(DEFAULT_URLS):
        raise ValueError("Four explicit endpoint identities required")
    for value in urls.values():
        parsed = urlsplit(value)
        if parsed.scheme != "https" or not parsed.hostname or not parsed.port or parsed.path:
            raise ValueError("HTTPS origins required")
    with tempfile.TemporaryDirectory(dir=root, prefix="issuer-") as temp:
        issuer = Path(temp)
        def openssl(*args):
            subprocess.run(["openssl", *args], cwd=issuer, check=True,
                           stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
        openssl("req", "-x509", "-newkey", "rsa:2048", "-nodes", "-keyout", "ca.key",
                "-out", "ca.crt", "-days", "2", "-subj", "/CN=GENESIS TEST CIRCLE CA")
        for name in [*DEFAULT_URLS, "control", "data", "outsider"]:
            openssl("req", "-newkey", "rsa:2048", "-nodes", "-keyout", name + ".key",
                    "-out", name + ".csr", "-subj", "/CN=" + name)
            usage = "serverAuth" if name in DEFAULT_URLS else "clientAuth"
            extensions = f"basicConstraints=CA:FALSE\nkeyUsage=digitalSignature,keyEncipherment\nextendedKeyUsage={usage}\n"
            if name in DEFAULT_URLS:
                extensions += "subjectAltName=DNS:" + urlsplit(urls[name]).hostname + "\n"
            (issuer / (name + ".ext")).write_text(extensions)
            openssl("x509", "-req", "-in", name + ".csr", "-CA", "ca.crt", "-CAkey", "ca.key",
                    "-CAcreateserial", "-out", name + ".crt", "-days", "2", "-extfile", name + ".ext")
        def pin(name):
            return hashlib.sha256(ssl.PEM_cert_to_DER_cert((issuer / (name + ".crt")).read_text())).hexdigest()
        def setup(service, identities):
            target = root / service
            target.mkdir(mode=0o700)
            shutil.copyfile(issuer / "ca.crt", target / "ca.crt")
            for name in identities:
                for ext in ("crt", "key"):
                    shutil.copyfile(issuer / (name + "." + ext), target / (name + "." + ext))
                (target / (name + ".key")).chmod(0o600)
            return target
        dirs = {name: setup(name, [name]) for name in DEFAULT_URLS}
        dirs["checker"] = setup("checker", ["control", "data", "outsider"])
        def tls(service, identity):
            base = Path("/run/genesis") if container_paths else dirs[service]
            return {"ca": str(base / "ca.crt"), "cert": str(base / (identity + ".crt")),
                    "key": str(base / (identity + ".key"))}
        def save(service, name, value):
            (dirs[service] / name).write_text(json.dumps(value, indent=2, sort_keys=True))
        parts = []
        for index, values in enumerate(((10, 20), (30, 40))):
            service = "source-a" if index == 0 else "source-b"
            raw = b"".join(dumps(value) + b"\n" for value in values)
            part_id = "part-" + str(index)
            (dirs[service] / (part_id + ".ndjson")).write_bytes(raw)
            parts.append({"part_id": part_id, "source_id": "source-" + str(index),
                          "origin": urls[service], "size": len(raw), "checksum": hashlib.sha256(raw).hexdigest(),
                          "record_hashes": [digest(value) for value in values]})
        spec = PACKAGE_SPECS["sector-a.numeric"]
        manifest = {"binding_id": "sector-a", "manifest_id": "circle-data", "data_version": "1",
                    "input_contract": spec["input_contract"], "format": "ndjson/1", "parts": parts}
        passport = {"binding_id": "sector-a", "level2_id": "sector-a.numeric", **spec,
                    "level3_manifest_id": manifest["manifest_id"], "data_version": "1",
                    "manifest_sha256": digest(manifest), "allowed_workers": ["worker-a", "worker-b"]}
        roles = {pin("control"): ["control"], pin("data"): ["data"]}
        for service in ("core-a", "core-b"):
            save(service, "server.json", {"tls": tls(service, service), "client_roles": roles})
            save(service, "manifest.json", manifest)
            save(service, "policy.json", {"bindings": [passport], "data_grants": {pin("data"): ["sector-a"]},
                                          "manifests": {"sector-a": "manifest.json"}})
        for service in ("source-a", "source-b"):
            save(service, "server.json", {"tls": tls(service, service), "client_pins": [pin("data")]})
            save(service, "manifest.json", manifest)
        peers = {urls[name]: [pin(name)] for name in ("core-a", "core-b")}
        for identity in ("control", "data", "outsider"):
            save("checker", identity + ".json", {"tls": tls("checker", identity), "peers": peers,
                                                  "endpoints": [urls["core-a"], urls["core-b"]], "timeout_s": 3})
        save("checker", "parts.json", {"tls": tls("checker", "data"),
                                      "peers": {urls[name]: [pin(name)] for name in ("source-a", "source-b")}})
        save("checker", "manifest.json", manifest)
        save("checker", "passport.json", passport)
    # The issuer directory, including the CA private key, is removed here.
    return root


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("directory")
    args = parser.parse_args()
    root = prepare(args.directory)
    # Runtime containers share only their own identity directory, under UID 10001.
    for path in sorted(root.rglob("*"), reverse=True):
        import os
        os.chown(path, 10001, 10001)
    print("Prepared synthetic circle. Test certificates expire in two days.")
