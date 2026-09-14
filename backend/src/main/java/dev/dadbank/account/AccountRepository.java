package dev.dadbank.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByAccountNumber(String accountNumber);
    Optional<Account> findByAccountNumber(String accountNumber);
    Optional<Account> findByOwnerIdAndType(Long userId, AccountType type);
    List<Account> findByOwnerId(Long userId);
    boolean existsByOwnerIdAndType(Long userId, AccountType type);

    default Optional<Account> findChecking(Long userId) { return findByOwnerIdAndType(userId, AccountType.CHECKING); }
    default Optional<Account> findSavings(Long userId) { return findByOwnerIdAndType(userId, AccountType.SAVINGS); }
}
