package dev.vgerasimov.account.internal;

import dev.vgerasimov.account.Account;
import dev.vgerasimov.account.AccountService;
import io.micronaut.context.annotation.Primary;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import jakarta.inject.Inject;

@Controller(value = "/account", produces = MediaType.APPLICATION_JSON)
final class AccountController {

  private final AccountService accountService;

  @Inject
  AccountController(@Primary AccountService accountService) {
    this.accountService = accountService;
  }

  @Get
  public Flowable<AccountResponse> getAll() {
    return accountService.getAll()
        .map(AccountResponse.ModelMapper::fromServiceAccount);
  }

  @Post
  public Single<CreateAccountResponse> createAccount(@Body CreateAccountRequest body) {
    return accountService.create(new AccountService.CreateRequest(body.name()))
        .map(CreateAccountResponse.ModelMapper::fromServiceNodeId);
  }

  static record AccountResponse(String id, String nodeId, String name) {
    public static class ModelMapper {
      public static AccountResponse fromServiceAccount(Account account) {
        return new AccountResponse(account.id().value(), account.nodeId().value(), account.name());
      }
    }
  }

  static record CreateAccountRequest(String name) {}

  static record CreateAccountResponse(String accountId) {
    public static class ModelMapper {
      public static CreateAccountResponse fromServiceNodeId(Account.Id accountId) {
        return new CreateAccountResponse(accountId.value());
      }
    }
  }
}
