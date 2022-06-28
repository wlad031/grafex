package dev.vgerasimov.graph.internal;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface NodeRepository {

  Single<String> save(Node node);

  Maybe<Node> getById(String nodeId);

  Flowable<Node> getAll();

  Flowable<Node> getByLabel(String label);

  Single<Boolean> delete(String nodeId);

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  final class Node {
    @BsonProperty("nodeId")
    private String id;
    private Set<String> labels;
    private Map<String, Object> properties;
    private List<String> children;
    private List<String> parents;

    public static class ModelMapper {
      public static dev.vgerasimov.graph.Node toServiceNode(Node node) {
        var id = new dev.vgerasimov.graph.Node.Id(node.getId());
        return new dev.vgerasimov.graph.Node(
            id,
            node.getLabels(),
            node.getProperties(),
            node.getChildren().stream()
                .map(r -> new dev.vgerasimov.graph.Node.Relationship(
                    id, new dev.vgerasimov.graph.Node.Id(r)))
                .collect(Collectors.toList()),
            node.getParents().stream()
                .map(r -> new dev.vgerasimov.graph.Node.Relationship(
                    new dev.vgerasimov.graph.Node.Id(r), id))
                .collect(Collectors.toList()));
      }
    }
  }
}
