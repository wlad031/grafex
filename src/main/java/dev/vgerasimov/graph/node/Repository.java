package dev.vgerasimov.graph.node;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.Map;
import java.util.Set;

public interface Repository {

  Single<String> save(Node node);

  Maybe<Node> getById(String nodeId);

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  final class Node {
    @BsonProperty("nodeId")
    private String id;
    private Set<String> labels;
    private Map<String, Object> properties;

    @Mapper
    public interface ModelMapper {
      @Mappings({
          @Mapping(target = "id.value", source = "id"),
          @Mapping(target = "labels", source = "labels"),
          @Mapping(target = "properties", source = "properties"),
      })
      dev.vgerasimov.graph.node.Node toServiceNode(Node node);
    }
  }
}
