package dev.vgerasimov.web;

import dev.vgerasimov.graph.node.Node;
import dev.vgerasimov.graph.node.NodeService;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import jakarta.inject.Inject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.Map;
import java.util.Set;

@Controller("/graph")
public class NodeController {

  private final NodeService nodeService;

  @Inject
  NodeController(NodeService nodeService) {
    this.nodeService = nodeService;
  }

  @Get(value = "/node/{nodeId}", produces = MediaType.APPLICATION_JSON)
  public Maybe<GetNodeByIdResponse> getNodeById(@PathVariable("nodeId") String nodeId) {
    return nodeService.getById(new Node.Id(nodeId))
        .map(Mappers.getMapper(GetNodeByIdResponse.ModelMapper.class)::fromServiceNode);
  }

  @Post(value = "/node", produces = MediaType.APPLICATION_JSON)
  public Single<CreateNodeResponse> createNode(@Body CreateNodeRequest body) {
    return nodeService.create(body.labels(), body.properties())
        .map(Mappers.getMapper(CreateNodeResponse.ModelMapper.class)::fromServiceNodeId);
  }

  public static record CreateNodeRequest(
      Set<String> labels,
      Map<String, Object> properties
  ) {}

  public static record CreateNodeResponse(String nodeId) {
    @Mapper
    public interface ModelMapper {
      @Mappings(@Mapping(target = "nodeId", source = "value"))
      CreateNodeResponse fromServiceNodeId(Node.Id nodeId);
    }
  }

  public static record GetNodeByIdResponse(
      String nodeId,
      Set<String> labels,
      Map<String, Object> properties
  ) {
    @Mapper
    public interface ModelMapper {
      @Mappings({
          @Mapping(target = "nodeId", source = "id.value"),
          @Mapping(target = "labels", source = "labels"),
          @Mapping(target = "properties", source = "properties"),
      })
      GetNodeByIdResponse fromServiceNode(Node node);
    }
  }
}
