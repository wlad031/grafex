package dev.vgerasimov;

import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

import java.util.Random;

@Factory
final class Configuration {

  @Singleton
  Random random() {
    return new Random();
  }
}
