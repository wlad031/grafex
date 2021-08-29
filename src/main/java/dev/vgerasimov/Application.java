package dev.vgerasimov;

import io.micronaut.runtime.Micronaut;

public class Application {

  public static void main(String[] args) {

    Micronaut.build(args)
        .eagerInitSingletons(false)
        .mainClass(Application.class)
        .start();
  }
}
