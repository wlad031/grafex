package dev.vgerasimov.graph;

import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoDatabase;
import dev.vgerasimov.graph.node.Repository;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.pojo.PojoCodecProvider;

@Factory
public final class Configuration {

  @Singleton
  @Named("graphDatabase")
  MongoDatabase graphDatabase(MongoClient mongoClient) {
    var pojoCodecRegistry = CodecRegistries.fromRegistries(
        MongoClientSettings.getDefaultCodecRegistry(),
        CodecRegistries.fromProviders(
            PojoCodecProvider.builder()
                .register(Repository.Node.class)
                .build()));
    return mongoClient.getDatabase("grafex").withCodecRegistry(pojoCodecRegistry);
  }
}
