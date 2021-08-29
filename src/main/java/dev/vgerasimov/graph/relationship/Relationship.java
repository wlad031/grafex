package dev.vgerasimov.graph.relationship;

import dev.vgerasimov.graph.node.Node;

import java.util.Map;

public record Relationship(
    Node.Id from,
    Node.Id to,
    String type,
    Map<String, String> properties
) {}
