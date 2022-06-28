package dev.vgerasimov.graph.internal;

import dev.vgerasimov.common.IdGenerator;
import dev.vgerasimov.graph.Node;
import dev.vgerasimov.graph.NodeService;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

import java.util.List;

final class LocalNodeService implements NodeService {

  private final NodeRepository repository;
  private final IdGenerator<Node.Id> idGenerator;

  LocalNodeService(NodeRepository repository, IdGenerator<Node.Id> idGenerator) {
    this.repository = repository;
    this.idGenerator = idGenerator;
  }

  @Override
  public Single<Node.Id> create(
      NodeService.CreateRequest request) {
    var id = idGenerator.generate();
    var nodeModel = new NodeRepository.Node(
        id.value(), request.labels(), request.properties(), List.of(), List.of());
    return repository.save(nodeModel).map(Node.Id::new);
  }

  @Override
  public Single<Boolean> deleteById(Node.Id id) {
    return repository.delete(id.value());
  }

  @Override
  public Maybe<Node> getById(Node.Id id) {
    return repository.getById(id.value())
        .map(NodeRepository.Node.ModelMapper::toServiceNode);
  }

  @Override
  public Flowable<Node> getAll() {
    return repository.getAll()
        .map(NodeRepository.Node.ModelMapper::toServiceNode);
  }

  @Override
  public Flowable<Node> getByLabel(String label) {
    return repository.getByLabel(label)
        .map(NodeRepository.Node.ModelMapper::toServiceNode);
  }
}
