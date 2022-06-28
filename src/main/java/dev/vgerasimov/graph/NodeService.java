package dev.vgerasimov.graph;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface NodeService {

  Single<Node.Id> create(CreateRequest request);

  record CreateRequest(Set<String> labels, Map<String, Object> properties) {}

  Single<Boolean> deleteById(Node.Id id);

  Maybe<Node> getById(Node.Id id);

  Flowable<Node> getAll();

  Flowable<Node> getByLabel(String label);


  record Nodes(Collection<Node> nodes) {}

  abstract sealed class Error extends RuntimeException permits Error.NodeCreationException {
    static final class NodeCreationException extends Error {}
  }

}
