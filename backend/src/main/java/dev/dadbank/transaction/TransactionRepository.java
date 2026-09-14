package dev.dadbank.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /** All rows touching one account (either side), newest first. Both parties and their owners are fetched eagerly. */
    @EntityGraph(attributePaths = {"fromAccount", "fromAccount.owner", "toAccount", "toAccount.owner"})
    Page<Transaction> findByFromAccountIdOrToAccountIdOrderByCreatedAtDescIdDesc(Long fromAccountId, Long toAccountId, Pageable pageable);

    default Page<Transaction> findByAccountId(Long accountId, Pageable pageable) {
        return findByFromAccountIdOrToAccountIdOrderByCreatedAtDescIdDesc(accountId, accountId, pageable);
    }
}
