import json
from pathlib import Path
import tempfile
import unittest

from experiments.finance_fixture_check.job import execute, validate


def job():
    return dict(version=1, job_id="demo-1", environment="offline-simulation",
                fixture_ids=["fixture-match", "fixture-missing"])


class JobTests(unittest.TestCase):
    def test_report_and_duplicate_protection(self):
        with tempfile.TemporaryDirectory() as directory:
            path = execute(job(), directory)
            original = path.read_bytes()
            self.assertEqual(json.loads(original)["counts"]["match"], 1)
            with self.assertRaises(FileExistsError):
                execute(job(), directory)
            self.assertEqual(path.read_bytes(), original)
            self.assertFalse(list(Path(directory).glob(".report-*")))

    def test_rejects_live_scope_and_paths(self):
        for key, value in [("environment", "production"), ("job_id", "../escape"),
                           ("fixture_ids", ["unknown"]),
                           ("fixture_ids", ["fixture-match", "fixture-match"])]:
            document = job()
            document[key] = value
            with self.assertRaises(ValueError):
                validate(document)

    def test_rejects_extra_commands(self):
        document = job()
        document["command"] = "arbitrary shell command"
        with self.assertRaises(ValueError):
            validate(document)
