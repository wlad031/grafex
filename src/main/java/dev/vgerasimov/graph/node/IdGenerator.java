package dev.vgerasimov.graph.node;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Random;

interface IdGenerator {
  Node.Id generate();

  final class RandomString implements IdGenerator {

    private final Random random;

    RandomString(Random random) {
      this.random = random;
    }

    @Override
    public Node.Id generate() {
      return new Node.Id(
          RandomStringUtils.random(
              /* count = */ 12,
              /* start = */ 0,
              /* end = */ 0,
              /* letters = */ true,
              /* numbers = */ true,
              /* chars = */ null,
              /* random = */ random));
    }
  }
}
