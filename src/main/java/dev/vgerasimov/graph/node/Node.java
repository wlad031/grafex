package dev.vgerasimov.graph.node;

import java.util.Map;
import java.util.Set;

public record Node(
    Id id,
    Set<String> labels,
    Map<String, Object> properties
) {
  public static record Id(String value) {}
}
