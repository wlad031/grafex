package dev.vgerasimov;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/hello")
public class HelloController {

  @Get(produces = MediaType.APPLICATION_JSON)
  public Object index() {
    return new Object() {
      {
        final String a = "1";
        final String b = "2";
      }
    };
  }
}
