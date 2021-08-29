package dev.vgerasimov;

import java.util.Optional;

public interface NodeService {
  Optional<Node> getById(String nodeId);
}
