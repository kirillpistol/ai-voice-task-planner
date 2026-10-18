from app.genesis_node_registry import Node, DEFAULT_NODES, eligible_nodes, node_counts


def test_pools_have_independent_capabilities():
    assert node_counts() == {"L2": {"declared": 1, "active": 0},
                             "L3": {"declared": 1, "active": 0}}
    assert eligible_nodes("L3", "mesm") == ()
    assert eligible_nodes("L2", "openai") == ()


def test_l2_load_cannot_route_to_l3():
    nodes = (Node("l2-b", "L2", frozenset({"openai"}), True),
             Node("l3-b", "L3", frozenset({"openai", "mesm"}), True))
    assert tuple(n.name for n in eligible_nodes("L2", "openai", nodes)) == ("l2-b",)
    assert eligible_nodes("L2", "mesm", nodes) == ()
    assert tuple(n.name for n in eligible_nodes("L3", "mesm", nodes)) == ("l3-b",)


def test_invalid_level_fails_closed():
    import pytest
    with pytest.raises(ValueError):
        eligible_nodes("L4", "mesm")
