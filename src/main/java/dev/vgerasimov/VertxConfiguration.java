package dev.vgerasimov;

import dev.vgerasimov.graph.NodeService;
import dev.vgerasimov.graph.NodeServiceVerticle;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Factory
final class VertxConfiguration {

  @Bean
  @Singleton
  public Vertx vertx() {
    return Vertx.vertx();
  }

  @Bean
  @Singleton
  public EventBus eventBus(Vertx vertx) {
    return vertx.eventBus();
  }

  @Bean
  public NodeServiceVerticle nodeServiceVerticle(
      EventBus eb,
      @Named("local") NodeService nodeService) {
    return new NodeServiceVerticle(eb, nodeService);
  }
}
