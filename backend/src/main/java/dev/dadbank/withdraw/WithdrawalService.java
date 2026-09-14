package dev.dadbank.withdraw;

import dev.dadbank.account.Account;
import dev.dadbank.account.AccountRepository;
import dev.dadbank.common.ApiException;
import dev.dadbank.transaction.Transaction;
import dev.dadbank.transaction.TransactionService;
import dev.dadbank.user.User;
import dev.dadbank.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WithdrawalService {

    public static final int MAX_PAGE_SIZE = 100;

    private final WithdrawalRepository withdrawalRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;

    public WithdrawalService(WithdrawalRepository withdrawalRepository, AccountRepository accountRepository,
                             UserRepository userRepository, TransactionService transactionService) {
        this.withdrawalRepository = withdrawalRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
    }

    /** Kid asks for cash. Checked against the balance now as a sanity check; the binding check happens on approval. */
    @Transactional
    public Withdrawal request(Long userId, long amountCents, String note) {
        if (amountCents <= 0) throw new ApiException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        Account account = requireChecking(userId);
        if (account.getBalanceCents() < amountCents) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient funds");
        }
        return withdrawalRepository.save(new Withdrawal(account, amountCents, clean(note)));
    }

    @Transactional
    public Page<Withdrawal> myRequests(Long userId, int page, int size) {
        Account account = requireChecking(userId);
        return withdrawalRepository.findByAccountIdOrderByCreatedAtDescIdDesc(account.getId(), pageable(page, size));
    }

    @Transactional
    public Page<Withdrawal> pendingQueue(int page, int size) {
        return withdrawalRepository.findByStatusOrderByCreatedAtAscIdAsc(WithdrawalStatus.PENDING, pageable(page, size));
    }

    /** Admin approves: money leaves the account and a WITHDRAWAL ledger row is written, all in one transaction. */
    @Transactional
    public Withdrawal approve(Long adminUserId, Long withdrawalId) {
        Withdrawal w = requirePending(withdrawalId);
        User admin = requireUser(adminUserId);
        Account account = w.getAccount();
        if (account.getBalanceCents() < w.getAmountCents()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient funds");
        }
        Transaction ledgerRow = transactionService.withdraw(account.getId(), w.getAmountCents(), w.getNote());
        w.approve(admin, ledgerRow);
        return w;
    }

    @Transactional
    public Withdrawal reject(Long adminUserId, Long withdrawalId, String reason) {
        Withdrawal w = requirePending(withdrawalId);
        User admin = requireUser(adminUserId);
        String r = clean(reason);
        if (r == null) throw new ApiException(HttpStatus.BAD_REQUEST, "A reason is required");
        w.reject(admin, r);
        return w;
    }

    private Withdrawal requirePending(Long withdrawalId) {
        Withdrawal w = withdrawalRepository.findById(withdrawalId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Withdrawal not found"));
        if (!w.isPending()) {
            throw new ApiException(HttpStatus.CONFLICT, "This request was already " + w.getStatus().name().toLowerCase());
        }
        return w;
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Account requireChecking(Long userId) {
        return accountRepository.findChecking(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "You have no account"));
    }

    private static PageRequest pageable(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, MAX_PAGE_SIZE)));
    }

    private static String clean(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
