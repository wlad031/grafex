package dev.vgerasimov.graph.internal;

import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.reactivestreams.client.MongoDatabase;
import dev.vgerasimov.common.Bson.$;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

final class MongoRepository implements NodeRepository {

  private final MongoDatabase database;

  private static final String NODES_DATABASE_NAME = "nodes";

  MongoRepository(MongoDatabase mongoDatabase) {
    this.database = mongoDatabase;
  }

  public static <A, B> Collector<A, ?, B> foldLeft(final B init, final BiFunction<? super B, ? super A, ? extends B> f) {
    return Collectors.collectingAndThen(
        Collectors.reducing(Function.<B>identity(), a -> b -> f.apply(b, a), Function::andThen),
        endo -> endo.apply(init)
    );
  }
  CodecRegistry codecRegistry;
  @Override
  public Single<String> save(Node node) {
    return Single.fromPublisher(
            database.getCollection(NODES_DATABASE_NAME)
                .updateOne(
                    $.doc().append("nodeId", $.str(node.getId())),
                    $.doc().append(
                        "$set",
                        $.doc()
                            .append("nodeId", $.str(node.getId()))
                            .append("labels", $.arr(node.getLabels(), $::str))
                            .append("properties", codecRegistry.get(Map.class).encode();)
                            .append("children", $.arr(node.getChildren(), $::str))
                            .append("parents", $.arr(node.getParents(), $::str))
                    ))
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
  public Flowable<Node> getByLabel(String label) {
    return Flowable.fromPublisher(
        database.getCollection(NODES_DATABASE_NAME, Node.class)
            .find(Filters.all("labels", label)));
  }

  @Override
  public Single<Boolean> delete(String nodeId) {
    return Single.fromPublisher(
            database.getCollection(NODES_DATABASE_NAME)
                .deleteOne(Filters.eq("nodeId", nodeId)))
        .map(DeleteResult::wasAcknowledged);
  }
}
