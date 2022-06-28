package dev.vgerasimov.graph.internal;

import dev.vgerasimov.graph.Node;
import dev.vgerasimov.graph.NodeService;
import io.micronaut.context.annotation.Primary;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.Set;

@Controller(value = "/graph/node", produces = MediaType.APPLICATION_JSON)
final class NodeController {

  private final NodeService nodeService;

  @Inject
  NodeController(@Primary NodeService nodeService) {
    this.nodeService = nodeService;
  }

  @Get(value = "/{nodeId}")
  public Maybe<NodeResponse> getById(@PathVariable("nodeId") String nodeId) {
    return nodeService.getById(new Node.Id(nodeId))
        .map(NodeResponse.ModelMapper::fromServiceNode);
  }

  @Get
  public Flowable<NodeResponse> getAll() {
    return nodeService.getAll()
        .map(NodeResponse.ModelMapper::fromServiceNode);
  }

  @Delete(value = "/{nodeId}")
  public Single<DeleteNodeResponse> deleteById(@PathVariable("nodeId") String nodeId) {
    return nodeService.deleteById(new Node.Id(nodeId))
        .map(DeleteNodeResponse::new);
  }

  @Post
  public Single<CreateNodeResponse> create(@Body CreateNodeRequest body) {
    return nodeService.create(new NodeService.CreateRequest(body.labels(), body.properties()))
        .map(CreateNodeResponse.ModelMapper::fromServiceNodeId);
  }

  public static record CreateNodeRequest(
      Set<String> labels,
      Map<String, Object> properties
  ) {}

  public static record CreateNodeResponse(String nodeId) {
    public static class ModelMapper {
      public static CreateNodeResponse fromServiceNodeId(Node.Id nodeId) {
        return new CreateNodeResponse(nodeId.value());
      }
    }
  }

  public static record DeleteNodeResponse(boolean success) {}

  public static record NodeResponse(
      String nodeId,
      Set<String> labels,
      Map<String, Object> properties
  ) {
    public static final class ModelMapper {
      public static NodeResponse fromServiceNode(Node node) {
        return new NodeResponse(node.id().value(), node.labels(), node.properties());
      }
    }
  }
}
