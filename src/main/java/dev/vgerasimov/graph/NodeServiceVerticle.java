package dev.vgerasimov.graph;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;

import java.util.stream.Collectors;

public final class NodeServiceVerticle extends AbstractVerticle {

  private final EventBus eventBus;
  private final NodeService delegate;

  public NodeServiceVerticle(EventBus eventBus, NodeService delegate) {
    this.eventBus = eventBus;
    this.delegate = delegate;
  }

  @Override
  public void start() throws Exception {
    eventBus.consumer("graph.node.create", this::create);
    eventBus.consumer("graph.node.deleteById", this::deleteById);
    eventBus.consumer("graph.node.getById", this::getById);
    eventBus.consumer("graph.node.getAll", this::getAll);
    eventBus.consumer("graph.node.getByLabel", this::getByLabel);
  }

  private void create(Message<Object> message) {
    var request = Json.decodeValue(message.body().toString(), NodeService.CreateRequest.class);
    delegate.create(request).subscribe(node -> message.reply(Json.encode(node)));
  }

  private void deleteById(Message<Object> message) {
    var nodeId = Json.decodeValue(message.body().toString(), Node.Id.class);
    delegate.deleteById(nodeId).subscribe(node -> message.reply(Json.encode(node)));
  }

  private void getById(Message<Object> message) {
    var nodeId = Json.decodeValue(message.body().toString(), Node.Id.class);
    delegate.getById(nodeId).subscribe(node -> message.reply(Json.encode(node)));
  }

  private void getAll(Message<Object> message) {
    delegate.getAll().collect(Collectors.toList())
        .subscribe(nodes -> message.reply(Json.encode(new NodeService.Nodes(nodes))));
  }

  private void getByLabel(Message<Object> message) {
    var label = Json.decodeValue(message.body().toString(), String.class);
    delegate.getByLabel(label).collect(Collectors.toList())
        .subscribe(nodes -> message.reply(Json.encode(new NodeService.Nodes(nodes))));
  }
}
