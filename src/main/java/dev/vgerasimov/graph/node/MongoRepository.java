package dev.vgerasimov.graph.node;

import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.reactivestreams.client.MongoDatabase;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

final class MongoRepository implements Repository {

  private final MongoDatabase database;

  private static final String NODES_DATABASE_NAME = "nodes";

  MongoRepository(MongoDatabase mongoDatabase) {
    this.database = mongoDatabase;
  }

  @Override
  public Single<String> save(Node node) {
    return Single.fromPublisher(
            database.getCollection(NODES_DATABASE_NAME, Node.class)
                .insertOne(node))
        .map(__ -> node.getId());
  }

  @Override
  public Maybe<Node> getById(String nodeId) {
    return Maybe.fromPublisher(
        database.getCollection(NODES_DATABASE_NAME, Node.class)
            .find(Filters.eq("nodeId", nodeId)));
  }

  @Override
  public Flowable<Node> getAll() {
    return Flowable.fromPublisher(
        database.getCollection(NODES_DATABASE_NAME, Node.class)
            .find());
  }

  @Override
  public Single<Boolean> delete(String nodeId) {
    return Single.fromPublisher(
            database.getCollection(NODES_DATABASE_NAME)
                .deleteOne(Filters.eq("nodeId", nodeId)))
        .map(DeleteResult::wasAcknowledged);
  }
}
