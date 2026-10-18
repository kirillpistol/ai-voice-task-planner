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
