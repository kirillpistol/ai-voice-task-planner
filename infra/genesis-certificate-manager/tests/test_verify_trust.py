"""Certificate trust checker negative tests (no secrets or Docker)."""
import importlib.util
import tempfile
import unittest
from pathlib import Path

path = Path(__file__).resolve().parents[1] / "verify_trust.py"
spec = importlib.util.spec_from_file_location("genesis_verify_trust", path)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class VerifyTrustTests(unittest.TestCase):
    def test_missing_certificates_fail_closed(self):
        with tempfile.TemporaryDirectory() as temp:
            result = module.validate(Path(temp))
        self.assertEqual(result["status"], "failed")
        self.assertEqual(len(result["failed_components"]), 6)
        self.assertFalse(result["rotation_enabled"])
        self.assertTrue(result["read_only"])

    def test_expected_dns_identity(self):
        self.assertEqual(module.EXPECTED["core-a"][0], "core-a")
        self.assertEqual(module.EXPECTED["source-b"][1], "9443")


if __name__ == "__main__":
    unittest.main()
