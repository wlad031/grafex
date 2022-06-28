package dev.vgerasimov.account;

import dev.vgerasimov.graph.Node;

public record Account(Id id, Node.Id nodeId, String name) {
  public static record Id(String value) {}
}
