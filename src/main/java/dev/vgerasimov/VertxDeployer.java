package dev.vgerasimov;

import dev.vgerasimov.graph.NodeServiceVerticle;
import io.vertx.core.Vertx;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class VertxDeployer {
  private static final Logger logger = LoggerFactory.getLogger(VertxDeployer.class);

  @Inject
  public VertxDeployer(Vertx vertx, NodeServiceVerticle nodeServiceVerticle) {
    logger.info("Verticles deployer created");

    vertx.deployVerticle(nodeServiceVerticle);
    logger.info("Verticles have been deployed");
  }
}
