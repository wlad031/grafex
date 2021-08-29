package dev.vgerasimov.graph.node;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.mapstruct.factory.Mappers;

import java.util.Map;
import java.util.Set;

public interface NodeService {

  Single<Node.Id> create(Set<String> labels, Map<String, Object> properties);

  Maybe<Node> getById(Node.Id id);

  abstract sealed class Error extends RuntimeException permits Error.NodeCreationException {
    static final class NodeCreationException extends Error {}
  }

  final class Local implements NodeService {

    final Repository repository;
    final IdGenerator nodeIdGenerator;

    Local(Repository repository, IdGenerator nodeIdGenerator) {
      this.repository = repository;
      this.nodeIdGenerator = nodeIdGenerator;
    }

    @Override
    public Single<Node.Id> create(
        Set<String> labels, Map<String, Object> properties) {
      var id = nodeIdGenerator.generate();
      var nodeModel = new Repository.Node(id.value(), labels, properties);
      return repository.save(nodeModel).map(Node.Id::new);
    }

    @Override
    public Maybe<Node> getById(Node.Id id) {
      return repository.getById(id.value())
          .map(Mappers.getMapper(Repository.Node.ModelMapper.class)::toServiceNode);
    }
  }
}
