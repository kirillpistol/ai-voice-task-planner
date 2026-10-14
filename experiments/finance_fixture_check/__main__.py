import argparse
import json
from .models import AndroidSession
from .simulator import FIXTURES, SimulatedAndroidAdapter
from .runner import run
from .report import report, save

parser = argparse.ArgumentParser(description="Offline Android finance fixture contract demo")
parser.add_argument("--report", help="Optional new JSON report file")
args = parser.parse_args()
session = AndroidSession("sberbank-test-adapter", "synthetic-test-session", True)
result = report(run(SimulatedAndroidAdapter(), session, FIXTURES, {row.case_id for row in FIXTURES}))
if args.report:
    save(result, args.report)
print(json.dumps(result, ensure_ascii=False, indent=2))
