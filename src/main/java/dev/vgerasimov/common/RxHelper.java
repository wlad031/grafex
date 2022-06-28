package dev.vgerasimov.common;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;

import java.util.Collection;
import java.util.function.Function;

public final class RxHelper {

  private RxHelper() {
  }

  public static <T, C> Flowable<T> singleCollectionToFlowable(
      Single<C> single,
      Function<C, Collection<T>> collectionExtractor) {
    return single.toFlowable().flatMap(c -> Flowable.fromStream(collectionExtractor.apply(c).stream()));
  }

  public static <C> io.reactivex.rxjava3.functions.Function<Message<?>, C> decodeBody(Class<C> clazz) {
    return message -> Json.decodeValue(message.body().toString(), clazz);
  }
}
