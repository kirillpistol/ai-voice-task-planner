from app.genesis_security_profiles import run_fixture_demo


def test_fixture_demo_results_and_metrics():
    data = run_fixture_demo(["demo-001", "demo-002", "demo-003", "demo-004"])
    assert data["mode"] == "local_fixture_simulation"
    assert data["isolated_execution"] is False
    assert data["positive"] == 2
    assert data["negative"] == 2
    assert data["errors"] == 0
    assert data["positive_rate"] == 0.5
    assert [item["result"] for item in data["results"]] == ["+", "-", "+", "-"]


def test_unknown_fixture_not_treated_as_negative():
    data = run_fixture_demo(["unknown"])
    assert data["negative"] == 0
    assert data["positive"] == 0
    assert data["errors"] == 1
    assert data["positive_rate"] is None


def test_batch_limit():
    import pytest
    with pytest.raises(ValueError):
        run_fixture_demo(["demo-001"] * 41)
