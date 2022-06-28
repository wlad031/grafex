package dev.vgerasimov.account.internal;

import dev.vgerasimov.graph.Node;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonProperty;

public interface Repository {

  Single<String> save(Account account);

  Maybe<Account> getById(String accountId);

  Flowable<Account> getAll();

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  final class Account {
    @BsonProperty("accountId")
    private String id;
    @BsonProperty("nodeId")
    private String nodeId;
    private String name;

    public static class ModelMapper {

      public static Account fromServiceNode(dev.vgerasimov.account.Account account) {
        return new Account(account.id().value(), account.nodeId().value(), account.name());
      }

      public static dev.vgerasimov.account.Account toServiceNode(Account account) {
        return new dev.vgerasimov.account.Account(
            new dev.vgerasimov.account.Account.Id(account.getId()),
            new Node.Id(account.getNodeId()),
            account.getName());
      }
    }
  }
}
