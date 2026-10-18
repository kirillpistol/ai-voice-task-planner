from app.genesis_node_registry import Node, NodeHealth, DEFAULT_NODES, eligible_nodes, node_counts, select_node


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


def test_selects_least_loaded_healthy_node_within_l2():
    nodes = (
        Node("l2-a", "L2", frozenset({"openai"}), True),
        Node("l2-b", "L2", frozenset({"openai"}), True),
        Node("l3-a", "L3", frozenset({"openai", "mesm"}), True),
    )
    health = (
        NodeHealth("l2-a", True, 7, 10),
        NodeHealth("l2-b", True, 1, 10),
        NodeHealth("l3-a", True, 0, 10),
    )
    assert select_node("L2", "openai", health, nodes).name == "l2-b"


def test_full_or_unhealthy_l2_does_not_fall_over_to_l3():
    nodes = (
        Node("l2-a", "L2", frozenset({"openai"}), True),
        Node("l3-a", "L3", frozenset({"openai"}), True),
    )
    health = (NodeHealth("l2-a", True, 10, 10),
              NodeHealth("l3-a", True, 0, 10))
    assert select_node("L2", "openai", health, nodes) is None
    assert select_node("L3", "openai", health, nodes).name == "l3-a"


def test_unverified_node_is_never_implicitly_healthy():
    nodes = (Node("l2-a", "L2", frozenset({"openai"}), True),)
    assert select_node("L2", "openai", (), nodes) is None


def test_inactive_node_and_invalid_capacity_are_rejected():
    import pytest
    nodes = (Node("l3-a", "L3", frozenset({"mesm"}), False),)
    assert select_node("L3", "mesm", (NodeHealth("l3-a", True, 0, 4),), nodes) is None
    with pytest.raises(ValueError):
        NodeHealth("l3-a", True, 0, 0)
