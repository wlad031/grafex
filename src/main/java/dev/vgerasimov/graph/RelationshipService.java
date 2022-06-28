package dev.vgerasimov.graph;

import io.reactivex.rxjava3.core.Single;

public interface RelationshipService {

  Single<Boolean> create(CreateRequest request);

  record CreateRequest(
      Node.Id fromNodeId,
      Node.Id toNodeId
  ) {}
}
