import csv
import io
import pytest
from app.genesis_dataset_store import DatasetStore


def test_template_import_compare_and_observation(tmp_path):
    store = DatasetStore(tmp_path / "datasets.sqlite")
    template = store.register_template("LAB", "test_cases", ["record_id", "scenario"])
    exported = store.export_template(template["template_id"])
    assert exported["revision"] == 1
    data = "record_id,scenario\ndemo-001,positive\ndemo-002,negative\n"
    imported = store.import_csv(template["template_id"], data)
    rows = store.compare(imported["dataset_id"])
    assert [x["status"] for x in rows] == ["pending", "pending"]
    store.record_observation(imported["dataset_id"], "demo-001", "owned.test.app", "1",
                             {"outcome": "positive"})
    rows = store.compare(imported["dataset_id"])
    assert rows[0]["result"]["outcome"] == "positive"
    assert rows[1]["status"] == "pending"


def test_rejects_mismatched_header_and_duplicate_record(tmp_path):
    store = DatasetStore(tmp_path / "db.sqlite")
    t = store.register_template("LAB", "cases", ["record_id", "scenario"])
    with pytest.raises(ValueError, match="header"):
        store.import_csv(t["template_id"], "wrong,scenario\ndemo-001,one\n")
    with pytest.raises(ValueError, match="duplicate"):
        store.import_csv(t["template_id"],
                         "record_id,scenario\ndemo-001,one\ndemo-001,two\n")


def test_ui_revision_and_proposal(tmp_path):
    store = DatasetStore(tmp_path / "db.sqlite")
    old = store.add_ui_revision("fixture", {"input": "case_input"})
    new = store.add_ui_revision("fixture", {"input": "case_number"})
    assert old["fingerprint"] != new["fingerprint"]
    proposal = store.propose_adaptation("fixture", old["revision_id"],
                                         new["revision_id"], {"input": "case_number"})
    assert len(proposal) == 32
