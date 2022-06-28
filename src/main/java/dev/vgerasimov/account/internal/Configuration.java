package dev.vgerasimov.account.internal;

import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoDatabase;
import dev.vgerasimov.account.Account;
import dev.vgerasimov.account.AccountService;
import dev.vgerasimov.common.IdGenerator;
import dev.vgerasimov.graph.NodeService;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.Random;

@Factory
final class Configuration {

  @Singleton
  @Named("accountDatabase")
  MongoDatabase accountDatabase(MongoClient mongoClient) {
    var pojoCodecRegistry = CodecRegistries.fromRegistries(
        MongoClientSettings.getDefaultCodecRegistry(),
        CodecRegistries.fromProviders(
            PojoCodecProvider.builder()
                .register(dev.vgerasimov.account.internal.Repository.Account.class)
                .build()));
    return mongoClient.getDatabase("grafex").withCodecRegistry(pojoCodecRegistry);
  }

  @Singleton
  Repository nodeRepository(@Named("accountDatabase") MongoDatabase database) {
    return new MongoRepository(database);
  }

  @Singleton
  @Primary
  @Named("local")
  AccountService accountService(
      Repository repository,
      IdGenerator<Account.Id> idGenerator,
      @Named("vertx") NodeService nodeService) {
    return new LocalAccountService(repository, idGenerator, nodeService);
  }

  @Singleton
  IdGenerator<Account.Id> accountIdGenerator(Random random) {
    return IdGenerator.RandomString.builder()
        .count(12)
        .letters(false)
        .numbers(true)
        .seed(random)
        .build()
        .map(s -> "acc" + s)
        .map(Account.Id::new);
  }
}
