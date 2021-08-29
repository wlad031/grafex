package dev.vgerasimov;

import java.util.Map;

public record Relationship(
    String fromNodeId,
    String toNodeId,
    String type,
    Map<String, String> properties
) {
}
