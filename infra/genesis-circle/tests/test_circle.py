"""Verify the exact deployment scripts over real loopback mTLS, without Docker."""
import importlib.util
import json
import os
import socket
import sys
import tempfile
import threading
import unittest
from pathlib import Path

from level1_core.bindings import BindingPolicy
from level1_core.runtime import Core
from level1_core.server import Server, server_context
from level2_algorithms.bindings_demo import REGISTRY, PACKAGE_SPECS
from level3_data.serve_parts import PartServer

ROOT = Path(__file__).resolve().parents[1]


def module(name, filename):
    spec = importlib.util.spec_from_file_location(name, ROOT / filename)
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value


prepare = module("circle_prepare", "prepare.py")
check = module("circle_check", "check.py")


class CircleTests(unittest.TestCase):
    def test_real_circle_identity_scope_handoff_and_repeat(self):
        with tempfile.TemporaryDirectory() as temp:
            # Reserve separate local ports while issuing their endpoint identities.
            reservations = []
            urls = {}
            for name in prepare.DEFAULT_URLS:
                sock = socket.socket()
                sock.bind(("127.0.0.1", 0))
                reservations.append(sock)
                urls[name] = "https://localhost:" + str(sock.getsockname()[1])
            root = prepare.prepare(temp, urls, container_paths=False)
            for sock in reservations:
                sock.close()
            servers, threads = [], []
            def load(service, filename):
                return json.loads((root / service / filename).read_text())
            try:
                for name in ("core-a", "core-b"):
                    config = load(name, "server.json")
                    worker = "worker-a" if name == "core-a" else "worker-b"
                    policy = BindingPolicy.load(root / name / "policy.json")
                    port = int(urls[name].rsplit(":", 1)[1])
                    servers.append(Server(("127.0.0.1", port), Core(REGISTRY, policy, PACKAGE_SPECS, worker),
                                          server_context(config["tls"]), client_roles=config["client_roles"]))
                for i, name in enumerate(("source-a", "source-b")):
                    config = load(name, "server.json")
                    port = int(urls[name].rsplit(":", 1)[1])
                    part = "part-" + str(i)
                    servers.append(PartServer(("127.0.0.1", port), server_context(config["tls"]), config["client_pins"],
                                               {"/v1/data/parts/" + part: root / name / (part + ".ndjson")}))
                threads = [threading.Thread(target=server.serve_forever, daemon=True) for server in servers]
                for thread in threads:
                    thread.start()
                report = check.run(root / "checker")
                self.assertEqual(report["status"], "passed")
                self.assertEqual(report["result"]["mean"], 25)
                self.assertEqual(report["repeat_records"], 0)
                # Each runtime receives only its own secret keys; the CA key is gone.
                for name in ("core-a", "core-b", "source-a", "source-b"):
                    self.assertEqual([p.name for p in (root / name).glob("*.key")], [name + ".key"])
                self.assertEqual(list(root.rglob("ca.key")), [])
                self.assertFalse(report["model_training"])
            finally:
                for server in servers:
                    if threads:
                        server.shutdown()
                    server.server_close()
                for thread in threads:
                    thread.join(timeout=5)

    def test_existing_identity_directory_is_not_overwritten(self):
        with tempfile.TemporaryDirectory() as temp:
            marker = Path(temp) / "existing"
            marker.write_text("keep")
            with self.assertRaises(ValueError):
                prepare.prepare(temp)
            self.assertEqual(marker.read_text(), "keep")


if __name__ == "__main__":
    unittest.main()
