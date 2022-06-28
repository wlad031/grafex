package dev.vgerasimov.common;

import lombok.Builder;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface IdGenerator<T> extends Supplier<T> {
  T generate();

  @Override
  default T get() {
    return generate();
  }

  default <S> IdGenerator<S> map(Function<T, S> mapper) {
    return () -> mapper.apply(generate());
  }

  @Builder
  final class RandomString implements IdGenerator<String> {
    private final int count;
    private final int start;
    private final int end;
    private final boolean letters;
    private final boolean numbers;
    private final char[] chars;
    private final Random seed;

    @Override
    public String generate() {
      return RandomStringUtils.random(count, start, end, letters, numbers, chars, seed);
    }
  }
}
