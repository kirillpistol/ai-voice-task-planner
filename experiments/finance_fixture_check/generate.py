"""Generate a synthetic job ID and select bundled test cases, never phone numbers."""
import argparse
import json
from pathlib import Path
import uuid

from .simulator import FIXTURES


def generate():
    return {
        'version': 1,
        'job_id': 'synthetic-' + uuid.uuid4().hex,
        'environment': 'offline-simulation',
        'fixture_ids': [row.case_id for row in FIXTURES],
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    try:
        with Path(args.output).open('x', encoding='utf-8') as file:
            json.dump(generate(), file, indent=2)
    except OSError:
        print(json.dumps({'status': 'file_not_created'}))
        return 1
    print(json.dumps({'status': 'created'}))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
