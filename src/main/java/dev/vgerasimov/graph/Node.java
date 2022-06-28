package dev.vgerasimov.graph;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record Node(
    Id id,
    Set<String> labels,
    Map<String, Object> properties,
    List<Relationship> children,
    List<Relationship> parents
) {
  public static record Id(String value) {}

  public record Relationship(
      Id fromNodeId,
      Id toNodeId
  ) {}
}
