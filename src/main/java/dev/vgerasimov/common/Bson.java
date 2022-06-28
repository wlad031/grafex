package dev.vgerasimov.common;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Bson {

  private Bson() {
  }

  public static class $ {

    public static BsonString str(String value) {
      return new BsonString(value);
    }

    public static <T> BsonArray arr(Collection<T> values, Function<T, BsonValue> f) {
      return new BsonArray(values.stream().map(f).collect(Collectors.toList()));
    }

    public static BsonDocument doc() {
      return new BsonDocument();
    }
  }
}
