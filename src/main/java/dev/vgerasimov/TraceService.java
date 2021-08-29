package dev.vgerasimov;

import io.micronaut.http.HttpRequest;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TraceService {

  private static final Logger logger = LoggerFactory.getLogger(TraceService.class);

  Flowable<Boolean> trace(HttpRequest<?> request) {
    return Flowable.fromCallable(() -> {
      if (logger.isDebugEnabled()) {
        logger.debug("Tracing request: " + request.getUri());
      }
      // trace logic here, potentially performing I/O
      return true;
    }).subscribeOn(Schedulers.io());
  }
}