import tempfile
from pathlib import Path
import sqlite3
import unittest
from unittest.mock import patch
from experiments.finance_fixture_check.worker import Queue, work_once


def document():
    return dict(version=1, job_id='worker-demo', environment='offline-simulation',
                fixture_ids=['fixture-match'])


class WorkerTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.queue = Queue(self.root / 'queue.db')

    def test_end_to_end_persists_and_does_not_repeat(self):
        self.queue.submit(document())
        self.assertTrue(work_once(self.queue, self.root / 'reports'))
        reopened = Queue(self.root / 'queue.db')
        self.assertEqual(reopened.status()[0]['state'], 'completed')
        self.assertTrue((self.root / 'reports/worker-demo.json').is_file())
        self.assertFalse(work_once(reopened, self.root / 'reports'))
        with self.assertRaises(sqlite3.IntegrityError):
            reopened.submit(document())

    def test_pending_cancellation(self):
        self.queue.submit(document())
        self.assertTrue(self.queue.cancel('worker-demo'))
        self.assertFalse(work_once(self.queue, self.root / 'reports'))

    def test_claim_is_exclusive_and_crash_is_not_retried(self):
        self.queue.submit(document())
        self.assertIsNotNone(self.queue.claim())
        self.assertIsNone(Queue(self.root / 'queue.db').claim())
        self.assertFalse(self.queue.cancel('worker-demo'))

    @patch('experiments.finance_fixture_check.worker.execute', side_effect=RuntimeError('secret'))
    def test_failed_job_is_sanitized_and_not_retried(self, execute):
        self.queue.submit(document())
        work_once(self.queue, self.root / 'reports')
        self.assertEqual(self.queue.status()[0]['reason'], 'execution_failed')
        self.assertNotIn('secret', str(self.queue.status()))
        self.assertFalse(work_once(self.queue, self.root / 'reports'))
