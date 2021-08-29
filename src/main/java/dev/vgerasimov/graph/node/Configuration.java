package dev.vgerasimov.graph.node;

import com.mongodb.reactivestreams.client.MongoDatabase;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.Random;

@Factory
public final class Configuration {

  @Singleton
  Repository nodeRepository(@Named("graphDatabase") MongoDatabase database) {
    return new MongoRepository(database);
  }

  @Singleton
  NodeService nodeService(Repository nodeRepository, IdGenerator nodeIdGenerator) {
    return new NodeService.Local(nodeRepository, nodeIdGenerator);
  }

  @Singleton
  Random random() {
    return new Random();
  }

  @Singleton
  IdGenerator nodeIdGenerator(Random random) {
    return new IdGenerator.RandomString(random);
  }
}
