"""Pure unit tests for certificate-expiry classification; no Docker required."""
import importlib.util
import tempfile
import unittest
from datetime import datetime, timezone
from pathlib import Path
from unittest.mock import patch

MODULE_PATH = Path(__file__).resolve().parents[1] / "certificate_inventory.py"
spec = importlib.util.spec_from_file_location("certificate_inventory", MODULE_PATH)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class CertificateInventoryTests(unittest.TestCase):
    now = datetime(2026, 10, 25, 17, 0, tzinfo=timezone.utc)

    def test_expired_certificates_fail_closed(self):
        with patch.object(module.ssl._ssl, "_test_decode_cert", return_value={"notAfter": "Oct 16 16:53:09 2026 GMT"}):
            report = module.inspect_circle(Path("/no-real-access"), now=self.now)
        self.assertEqual(report["overall"], "critical")
        self.assertTrue(all(item["status"] == "expired" for item in report["certificates"]))
        self.assertFalse(report["rotation_enabled"])

    def test_warning_window(self):
        with patch.object(module.ssl._ssl, "_test_decode_cert", return_value={"notAfter": "Oct 27 16:53:09 2026 GMT"}):
            report = module.inspect_circle(Path("/no-real-access"), warning_days=7, now=self.now)
        self.assertEqual(report["overall"], "warning")

    def test_invalid_file_fails_closed(self):
        with patch.object(module.ssl._ssl, "_test_decode_cert", side_effect=OSError("missing")):
            report = module.inspect_circle(Path("/no-real-access"), now=self.now)
        self.assertEqual(report["overall"], "critical")

    def test_future_certificate_is_ok(self):
        with patch.object(module.ssl._ssl, "_test_decode_cert", return_value={"notAfter": "Nov 30 16:53:09 2026 GMT"}):
            report = module.inspect_circle(Path("/no-real-access"), now=self.now)
        self.assertEqual(report["overall"], "ok")

    def test_negative_warning_rejected(self):
        with self.assertRaises(ValueError):
            module.inspect_circle(Path("/tmp"), warning_days=-1, now=self.now)


if __name__ == "__main__":
    unittest.main()
