"""Node registry for distinct L2 and L3 scaling pools."""
from dataclasses import dataclass


@dataclass(frozen=True)
class Node:
    name: str
    level: str
    capabilities: frozenset[str]
    active: bool = False


DEFAULT_NODES = (
    Node("l2-primary", "L2", frozenset({"api", "openai", "billing"})),
    Node("l3-primary", "L3", frozenset({"mesm", "med"})),
)


def eligible_nodes(level: str, capability: str, nodes=DEFAULT_NODES):
    if level not in ("L2", "L3"):
        raise ValueError("Unknown level")
    return tuple(n for n in nodes if n.level == level and n.active and capability in n.capabilities)


def node_counts(nodes=DEFAULT_NODES):
    return {level: {"declared": sum(n.level == level for n in nodes),
                    "active": sum(n.level == level and n.active for n in nodes)}
            for level in ("L2", "L3")}


@dataclass(frozen=True)
class NodeHealth:
    """Trusted monitoring snapshot, not values supplied by Android clients."""
    node_name: str
    available: bool
    running_jobs: int
    capacity: int

    def __post_init__(self):
        if self.running_jobs < 0 or self.capacity <= 0:
            raise ValueError("Invalid node capacity")


def select_node(level: str, capability: str, health: tuple[NodeHealth, ...],
                nodes=DEFAULT_NODES):
    """Pick the least occupied healthy node of the same level.

    Returns None when the requested pool has no available capacity;
    intentionally never tries a different level or starts execution.
    """
    candidates = eligible_nodes(level, capability, nodes)
    trusted = {h.node_name: h for h in health}
    ready = [(node, trusted[node.name]) for node in candidates
             if node.name in trusted and trusted[node.name].available
             and trusted[node.name].running_jobs < trusted[node.name].capacity]
    if not ready:
        return None
    return min(ready, key=lambda item: (
        item[1].running_jobs / item[1].capacity,
        item[1].running_jobs,
        item[0].name
    ))[0]
