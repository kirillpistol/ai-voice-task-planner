"""Quarterly purge of GENESIS SQL task copies, keeping recommendations.

Run from an authorised scheduler every January/April/July/October 1 (UTC).
Re-runs are safe: only copies imported BEFORE the new quarter are removed.
"""
from datetime import datetime, timezone

from sqlalchemy import delete

from app.genesis_store import GenesisTask, make_session_factory, configured_database_url


def purge_previous_quarters(now=None, session_factory=None) -> int:
    now = now or datetime.now(timezone.utc)
    first_month = ((now.month - 1) // 3) * 3 + 1
    boundary = datetime(now.year, first_month, 1, tzinfo=timezone.utc)
    factory = session_factory or make_session_factory(configured_database_url())
    with factory() as session:
        result = session.execute(
            delete(GenesisTask).where(GenesisTask.imported_at < boundary)
        )
        session.commit()
        return result.rowcount


if __name__ == "__main__":
    print(f"Deleted {purge_previous_quarters()} previous-quarter SQL copies")
