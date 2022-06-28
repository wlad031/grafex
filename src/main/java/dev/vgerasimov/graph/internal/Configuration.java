package dev.vgerasimov.graph.internal;

import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoDatabase;
import dev.vgerasimov.common.IdGenerator;
import dev.vgerasimov.graph.Node;
import dev.vgerasimov.graph.NodeService;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.vertx.core.eventbus.EventBus;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.Random;

@Factory
public final class Configuration {

  @Singleton
  @Named("graphDatabase")
  MongoDatabase graphDatabase(MongoClient mongoClient) {
    var pojoCodecRegistry = CodecRegistries.fromRegistries(
        MongoClientSettings.getDefaultCodecRegistry(),
        CodecRegistries.fromProviders(
            PojoCodecProvider.builder()
                .register(NodeRepository.Node.class)
                .build()));
    return mongoClient.getDatabase("grafex").withCodecRegistry(pojoCodecRegistry);
  }

  @Singleton
  NodeRepository nodeRepository(@Named("graphDatabase") MongoDatabase database) {
    return new MongoRepository(database);
  }

  @Singleton
  @Primary
  @Named("vertx")
  VertxNodeService nodeService(EventBus eb) {
    return new VertxNodeService(eb);
  }

  @Singleton
  @Named("local")
  NodeService nodeService(NodeRepository nodeRepository, IdGenerator<Node.Id> nodeIdGenerator) {
    return new LocalNodeService(nodeRepository, nodeIdGenerator);
  }

  @Singleton
  IdGenerator<Node.Id> nodeIdGenerator(Random random) {
    return IdGenerator.RandomString.builder()
        .count(12)
        .letters(true)
        .numbers(true)
        .seed(random)
        .build()
        .map(Node.Id::new);
  }
}
