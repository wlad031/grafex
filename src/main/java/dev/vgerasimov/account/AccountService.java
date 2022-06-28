package dev.vgerasimov.account;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

public interface AccountService {

  Single<Account.Id> create(CreateRequest request);

  Flowable<Account> getAll();

  record CreateRequest(String name) {}
}
