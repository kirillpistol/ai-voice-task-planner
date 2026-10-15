"""Local SQLite queue for synthetic jobs. No network or Android UI actions."""
import argparse
import json
from pathlib import Path
import sqlite3
import time

from .job import execute, validate


class Queue:
    def __init__(self, path):
        self.path = str(path)
        with self.connect() as db:
            db.execute("""CREATE TABLE IF NOT EXISTS jobs (
                job_id TEXT PRIMARY KEY, document TEXT NOT NULL,
                state TEXT NOT NULL CHECK(state IN
                    ('pending','running','completed','failed','cancelled')),
                duration_ms INTEGER, reason TEXT)""")

    def connect(self):
        return sqlite3.connect(self.path, timeout=10)

    def submit(self, document):
        job_id, _ = validate(document)
        with self.connect() as db:
            db.execute("INSERT INTO jobs VALUES (?, ?, 'pending', NULL, NULL)",
                       (job_id, json.dumps(document)))
        return job_id

    def cancel(self, job_id):
        with self.connect() as db:
            return db.execute("UPDATE jobs SET state='cancelled' WHERE job_id=? AND state='pending'",
                              (job_id,)).rowcount == 1

    def claim(self):
        with self.connect() as db:
            db.execute("BEGIN IMMEDIATE")
            row = db.execute("SELECT job_id, document FROM jobs WHERE state='pending' ORDER BY rowid LIMIT 1").fetchone()
            if row is None:
                return None
            db.execute("UPDATE jobs SET state='running' WHERE job_id=?", (row[0],))
            return row[0], json.loads(row[1])

    def finish(self, job_id, state, duration_ms, reason=None):
        if state not in {'completed', 'failed'}:
            raise ValueError('Invalid terminal state')
        with self.connect() as db:
            changed = db.execute("""UPDATE jobs SET state=?,duration_ms=?,reason=?
                WHERE job_id=? AND state='running'""",
                (state, duration_ms, reason, job_id)).rowcount
            if changed != 1:
                raise ValueError('Job is not running')

    def status(self):
        with self.connect() as db:
            return [dict(zip(('job_id', 'state', 'duration_ms', 'reason'), row))
                    for row in db.execute('SELECT job_id,state,duration_ms,reason FROM jobs ORDER BY rowid')]


def work_once(queue, reports):
    item = queue.claim()
    if item is None:
        return False
    job_id, document = item
    started = time.monotonic()
    try:
        execute(document, reports)
    except Exception:
        # Never export exception payloads, application contents or credentials.
        queue.finish(job_id, 'failed', round((time.monotonic()-started)*1000), 'execution_failed')
    else:
        queue.finish(job_id, 'completed', round((time.monotonic()-started)*1000))
    return True


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--workspace', required=True)
    commands = parser.add_subparsers(dest='action', required=True)
    submit = commands.add_parser('submit')
    submit.add_argument('--job', required=True)
    commands.add_parser('run-once')
    commands.add_parser('status')
    cancel = commands.add_parser('cancel')
    cancel.add_argument('--job-id', required=True)
    args = parser.parse_args()
    root = Path(args.workspace)
    root.mkdir(parents=True, exist_ok=True)
    queue = Queue(root / 'queue.sqlite3')
    try:
        if args.action == 'submit':
            with Path(args.job).open('rb') as file:
                raw = file.read(16385)
            if len(raw) > 16384:
                raise ValueError('Job too large')
            result = {'submitted': queue.submit(json.loads(raw))}
        elif args.action == 'run-once':
            result = {'processed': work_once(queue, root / 'reports')}
        elif args.action == 'cancel':
            result = {'cancelled': queue.cancel(args.job_id)}
        else:
            result = {'jobs': queue.status()}
    except (ValueError, OSError, sqlite3.Error):
        print(json.dumps({'status': 'rejected_or_failed'}))
        return 1
    print(json.dumps(result))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
