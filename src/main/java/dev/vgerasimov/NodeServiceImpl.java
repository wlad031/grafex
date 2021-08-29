package dev.vgerasimov;

import com.mongodb.reactivestreams.client.MongoClient;

import java.util.Optional;

import static com.mongodb.client.model.Projections.include;

final class NodeServiceImpl implements NodeService {
  final MongoClient mongoClient;

  NodeServiceImpl(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
  }

  @Override
  public Optional<Node> getById(String nodeId) {
    var foo =
        mongoClient
            .getDatabase("grafex")
            .getCollection("nodes")
            .find()
            .projection(include("id", "labels", "properties"));
    System.out.println(foo);
    return Optional.empty();
  }
}
