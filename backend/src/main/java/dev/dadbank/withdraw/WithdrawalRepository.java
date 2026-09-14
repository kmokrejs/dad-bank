package dev.dadbank.withdraw;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {

    /** A kid's requests, every status, newest first. */
    @EntityGraph(attributePaths = {"account", "account.owner", "decidedBy"})
    Page<Withdrawal> findByAccountIdOrderByCreatedAtDescIdDesc(Long accountId, Pageable pageable);

    /** Admin queue: oldest first so nobody waits forever. */
    @EntityGraph(attributePaths = {"account", "account.owner"})
    Page<Withdrawal> findByStatusOrderByCreatedAtAscIdAsc(WithdrawalStatus status, Pageable pageable);

    long countByAccountIdAndStatus(Long accountId, WithdrawalStatus status);
}
