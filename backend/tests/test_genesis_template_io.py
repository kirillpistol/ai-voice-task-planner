import csv
from app.genesis_dataset_store import DatasetStore
from app.genesis_template_io import export_template_files, template_csv


def test_export_downloadable_template_files(tmp_path):
    store = DatasetStore(tmp_path / "db.sqlite")
    template = store.register_template("LAB", "cases", ["record_id", "scenario"])
    assert list(csv.reader(template_csv(store, template["template_id"]).splitlines())) == [
        ["record_id", "scenario"]
    ]
    paths = export_template_files(store, template["template_id"], tmp_path / "export")
    assert (tmp_path / "export" / "LAB_cases_v1.csv").exists()
    assert (tmp_path / "export" / "LAB_cases_v1.template.json").exists()
