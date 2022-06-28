package dev.vgerasimov.graph.internal;

import dev.vgerasimov.graph.Node;
import dev.vgerasimov.graph.NodeService;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;

import static dev.vgerasimov.common.RxHelper.decodeBody;
import static dev.vgerasimov.common.RxHelper.singleCollectionToFlowable;
import static io.vertx.rxjava3.MaybeHelper.toMaybe;
import static io.vertx.rxjava3.SingleHelper.toSingle;

final class VertxNodeService implements NodeService {

  private final EventBus eventBus;

  public VertxNodeService(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  @Override
  public Single<Node.Id> create(
      NodeService.CreateRequest request) {
    return toSingle(
        (Handler<AsyncResult<Message<Object>>> handler) ->
            eventBus.request(
                "graph.node.create",
                Json.encode(request),
                handler))
        .map(decodeBody(Node.Id.class));
  }

  @Override
  public Single<Boolean> deleteById(Node.Id id) {
    return toSingle(
        (Handler<AsyncResult<Message<Object>>> handler) ->
            eventBus.request(
                "graph.node.deleteById",
                Json.encode(id),
                handler))
        .map(decodeBody(Boolean.class));
  }

  @Override
  public Maybe<Node> getById(Node.Id id) {
    return toMaybe(
        (Handler<AsyncResult<Message<Object>>> handler) ->
            eventBus.request(
                "graph.node.getById",
                Json.encode(id),
                handler))
        .map(decodeBody(Node.class));
  }

  @Override
  public Flowable<Node> getAll() {
    return singleCollectionToFlowable(
        toSingle(
            (Handler<AsyncResult<Message<Object>>> handler) ->
                eventBus.request(
                    "graph.node.getAll",
                    Json.encode(null),
                    handler))
            .map(decodeBody(NodeService.Nodes.class)),
        Nodes::nodes);
  }

  @Override
  public Flowable<Node> getByLabel(String label) {
    return singleCollectionToFlowable(
        toSingle(
            (Handler<AsyncResult<Message<Object>>> handler) ->
                eventBus.request(
                    "graph.node.getByLabel",
                    Json.encode(label),
                    handler))
            .map(decodeBody(NodeService.Nodes.class)),
        Nodes::nodes);
  }
}
