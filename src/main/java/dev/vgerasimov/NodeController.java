package dev.vgerasimov;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import jakarta.inject.Inject;

import java.util.Optional;

@Controller("/graph")
public class NodeController {

  private final NodeService nodeService;

  @Inject
  public NodeController(NodeService nodeService) {
    this.nodeService = nodeService;
  }

  @Get(value = "/node/{nodeId}", produces = MediaType.APPLICATION_JSON)
  public Optional<Node> getNodeById(@PathVariable("nodeId") String nodeId) {
    System.out.println("hello");
    return nodeService.getById(nodeId);
  }
}
