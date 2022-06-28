package dev.vgerasimov.account.internal;

import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoDatabase;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

final class MongoRepository implements Repository {

  private final MongoDatabase database;

  private static final String ACCOUNTS_DATABASE_NAME = "accounts";

  MongoRepository(MongoDatabase mongoDatabase) {
    this.database = mongoDatabase;
  }

  @Override
  public Single<String> save(Account account) {
    return Single.fromPublisher(
            database.getCollection(ACCOUNTS_DATABASE_NAME, Account.class)
                .insertOne(account))
        .map(__ -> account.getId());
  }

  @Override
  public Maybe<Account> getById(String accountId) {
    return Maybe.fromPublisher(
        database.getCollection(
                ACCOUNTS_DATABASE_NAME,
                Account.class)
            .find(Filters.eq("accountId", accountId)));
  }

  @Override
  public Flowable<Account> getAll() {
    return Flowable.fromPublisher(
        database.getCollection(ACCOUNTS_DATABASE_NAME, Account.class)
            .find());
  }
}
