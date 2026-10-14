import json
import tempfile
import unittest
from pathlib import Path
from experiments.finance_fixture_check.models import AndroidSession, Fixture, Observation, Verdict
from experiments.finance_fixture_check.runner import run, compare
from experiments.finance_fixture_check.simulator import SimulatedAndroidAdapter, FIXTURES
from experiments.finance_fixture_check.report import report, save


class FixtureTests(unittest.TestCase):
    def setUp(self):
        self.session = AndroidSession("test-app", "test-account-ref", True)
        self.ids = {row.case_id for row in FIXTURES}
    def test_complete_cycle_and_cleanup(self):
        adapter = SimulatedAndroidAdapter()
        rows = run(adapter, self.session, FIXTURES, self.ids)
        self.assertEqual([row.verdict for row in rows], [Verdict.MATCH, Verdict.MISMATCH, Verdict.NOT_FOUND])
        self.assertTrue(adapter.closed)
        document = report(rows)
        self.assertEqual(document["counts"]["match"], 1)
        self.assertNotIn("Получатель", json.dumps(document, ensure_ascii=False))
    def test_rejects_undeclared_cases_before_opening_session(self):
        adapter = SimulatedAndroidAdapter()
        with self.assertRaises(ValueError):
            run(adapter, self.session, FIXTURES, set())
        self.assertFalse(adapter.opened)
    def test_does_not_enable_live_adapters(self):
        adapter = SimulatedAndroidAdapter()
        adapter.environment = "live-android"
        with self.assertRaises(ValueError):
            run(adapter, self.session, FIXTURES, self.ids)
        self.assertFalse(adapter.opened)
    def test_comparison_normalizes_spacing_and_case(self):
        result = compare(Fixture("one", "Тестовый Получатель А."), Observation(True, " тестовый  получатель а. "))
        self.assertEqual(result.verdict, Verdict.MATCH)
    def test_report_does_not_overwrite_and_is_saved_before_disposal(self):
        document = report(run(SimulatedAndroidAdapter(), self.session, FIXTURES, self.ids))
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "report.json"
            save(document, path)
            self.assertEqual(json.loads(path.read_text()), document)
            with self.assertRaises(FileExistsError):
                save(document, path)
