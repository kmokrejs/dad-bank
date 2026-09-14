package dev.dadbank.savings;

import dev.dadbank.account.Account;
import dev.dadbank.account.AccountNumberGenerator;
import dev.dadbank.account.AccountRepository;
import dev.dadbank.account.AccountType;
import dev.dadbank.common.ApiException;
import dev.dadbank.transaction.Transaction;
import dev.dadbank.transaction.TransactionService;
import dev.dadbank.user.User;
import dev.dadbank.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavingsService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final TransactionService transactionService;

    public SavingsService(AccountRepository accountRepository, UserRepository userRepository,
                          AccountNumberGenerator accountNumberGenerator, TransactionService transactionService) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.accountNumberGenerator = accountNumberGenerator;
        this.transactionService = transactionService;
    }

    /** Opens the user's single savings account. 409 if they already have one. */
    @Transactional
    public Account open(Long userId) {
        if (accountRepository.existsByOwnerIdAndType(userId, AccountType.SAVINGS)) {
            throw new ApiException(HttpStatus.CONFLICT, "You already have a savings account");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        Account savings = accountRepository.save(new Account(user, accountNumberGenerator.generateUnique(), AccountType.SAVINGS));
        user.addAccount(savings);
        return savings;
    }

    /** Checking → savings. */
    @Transactional
    public Transaction deposit(Long userId, long amountCents, String note) {
        return transactionService.move(checking(userId), savings(userId), amountCents, note);
    }

    /** Savings → checking. */
    @Transactional
    public Transaction withdraw(Long userId, long amountCents, String note) {
        return transactionService.move(savings(userId), checking(userId), amountCents, note);
    }

    public Account savings(Long userId) {
        return accountRepository.findSavings(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "You have no savings account yet"));
    }

    private Account checking(Long userId) {
        return accountRepository.findChecking(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "You have no account"));
    }
}
