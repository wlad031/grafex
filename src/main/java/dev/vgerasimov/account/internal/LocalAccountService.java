package dev.vgerasimov.account.internal;

import dev.vgerasimov.account.Account;
import dev.vgerasimov.account.AccountService;
import dev.vgerasimov.common.IdGenerator;
import dev.vgerasimov.graph.Node;
import dev.vgerasimov.graph.NodeService;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

final class LocalAccountService implements AccountService {
  private static final Logger logger = LoggerFactory.getLogger(LocalAccountService.class);

  private static final String NODE_LABEL_ACCOUNT = "ACCOUNT";

  private final Repository repository;
  private final IdGenerator<Account.Id> idGenerator;
  private final NodeService nodeService;

  LocalAccountService(
      Repository repository,
      IdGenerator<Account.Id> idGenerator,
      NodeService nodeService) {
    this.repository = repository;
    this.idGenerator = idGenerator;
    this.nodeService = nodeService;
  }

  @Override
  public Single<Account.Id> create(AccountService.CreateRequest request) {
    return nodeService.create(new NodeService.CreateRequest(Set.of(NODE_LABEL_ACCOUNT), Map.of()))
        .map(nodeId -> new Account(idGenerator.generate(), nodeId, request.name()))
        .map(Repository.Account.ModelMapper::fromServiceNode)
        .flatMap(account -> {
          try {
            return repository.save(account);
          } catch (Exception e) {
            logger.error("Cannot save account, reverting node creation");
            nodeService.deleteById(new Node.Id(account.getNodeId()));
            return Single.error(e);
          }
        })
        .map(Account.Id::new);
  }

  @Override
  public Flowable<Account> getAll() {
    return repository.getAll()
        .map(Repository.Account.ModelMapper::toServiceNode);
  }
}
