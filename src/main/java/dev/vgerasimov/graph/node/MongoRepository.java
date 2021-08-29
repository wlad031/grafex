package dev.vgerasimov.graph.node;

import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoDatabase;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

final class MongoRepository implements Repository {

  final MongoDatabase database;

  MongoRepository(MongoDatabase mongoDatabase) {
    this.database = mongoDatabase;
  }

  @Override
  public Single<String> save(Node node) {
    return Single.fromPublisher(database.getCollection("nodes1", Node.class).insertOne(node))
        .map(__ -> node.getId());
  }

  @Override
  public Maybe<Node> getById(String nodeId) {
    return Maybe.fromPublisher(
        database.getCollection("nodes", Node.class)
            .find(Filters.eq("nodeId", nodeId)));
  }
}
