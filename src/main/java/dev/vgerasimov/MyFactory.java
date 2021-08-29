package dev.vgerasimov;

import com.mongodb.reactivestreams.client.MongoClient;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

@Factory
public class MyFactory {

  @Singleton
  NodeService nodeService(MongoClient mongoClient) {
    return new NodeServiceImpl(mongoClient);
  }
}
