import unittest
from experiments.finance_fixture_check.generate import generate
from experiments.finance_fixture_check.job import validate


class GeneratorTests(unittest.TestCase):
    def test_generates_valid_distinct_jobs_without_phone_fields(self):
        first, second = generate(), generate()
        validate(first)
        validate(second)
        self.assertNotEqual(first['job_id'], second['job_id'])
        self.assertEqual(set(first), {'version', 'job_id', 'environment', 'fixture_ids'})
        self.assertTrue(all(case.startswith('fixture-') for case in first['fixture_ids']))
