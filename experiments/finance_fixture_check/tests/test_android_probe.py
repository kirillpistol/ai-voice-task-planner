import subprocess
import unittest
from unittest.mock import patch

from experiments.finance_fixture_check.android_probe import parse_devices, probe


class ProbeTests(unittest.TestCase):
    def test_ignores_adb_banner(self):
        self.assertEqual(parse_devices(
            "List of devices attached\n emulator-5554\tdevice\n"
            "other\toffline\n* daemon started successfully *\n"
        ), [{"serial": "emulator-5554", "state": "device"},
            {"serial": "other", "state": "offline"}])

    @patch("subprocess.run")
    def test_never_uses_another_device(self, run):
        run.return_value = subprocess.CompletedProcess([], 0, "other\tdevice\n")
        self.assertEqual(probe("adb", "selected"), {"status": "device_not_found"})
        self.assertEqual(run.call_count, 1)

    @patch("subprocess.run")
    def test_unauthorized_stops_before_shell(self, run):
        run.return_value = subprocess.CompletedProcess([], 0, "chosen\tunauthorized\n")
        self.assertEqual(probe("adb", "chosen"), {"status": "unauthorized"})
        self.assertEqual(run.call_count, 1)

    @patch("subprocess.run")
    def test_ready_uses_only_read_only_properties(self, run):
        run.side_effect = [subprocess.CompletedProcess([], 0, value)
                           for value in ("chosen\tdevice\n", "1", "9")]
        self.assertEqual(probe("adb", "chosen")["status"], "ready")
        for call in run.call_args_list:
            self.assertFalse(call.kwargs["shell"])
            self.assertEqual(call.kwargs["timeout"], 15)
        self.assertEqual(run.call_args_list[1].args[0],
                         ["adb", "-s", "chosen", "shell", "getprop", "sys.boot_completed"])
