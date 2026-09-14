package dev.dadbank.account;

import dev.dadbank.common.ApiException;
import dev.dadbank.common.PageResponse;
import dev.dadbank.transaction.Transaction;
import dev.dadbank.transaction.TransactionDtos.AdjustRequest;
import dev.dadbank.transaction.TransactionDtos.TransactionResult;
import dev.dadbank.transaction.TransactionDtos.TransactionView;
import dev.dadbank.transaction.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** God-mode endpoints. Access is restricted to ROLE_ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
public class AdminAccountController {

    public record AdminAccountView(Long accountId, Long userId, String username, String email, String role,
                                   AccountType accountType, String accountNumber, long balanceCents) {}

    private final AccountRepository accountRepository;
    private final TransactionService transactionService;

    public AdminAccountController(AccountRepository accountRepository, TransactionService transactionService) {
        this.accountRepository = accountRepository;
        this.transactionService = transactionService;
    }

    @GetMapping("/accounts")
    @Transactional
    public List<AdminAccountView> allAccounts() {
        return accountRepository.findAll().stream().map(AdminAccountController::toView).toList();
    }

    /** Put money into an account (pocket money, birthday gift, ...). */
    @PostMapping("/accounts/{accountId}/deposit")
    @Transactional
    public ResponseEntity<TransactionResult> deposit(@PathVariable Long accountId, @Valid @RequestBody AdjustRequest req) {
        Transaction t = transactionService.deposit(accountId, req.amountCents(), req.note());
        return created(t, accountId);
    }

    /** Take money out of an account (the kid spent it in the real world). */
    @PostMapping("/accounts/{accountId}/withdraw")
    @Transactional
    public ResponseEntity<TransactionResult> withdraw(@PathVariable Long accountId, @Valid @RequestBody AdjustRequest req) {
        Transaction t = transactionService.withdraw(accountId, req.amountCents(), req.note());
        return created(t, accountId);
    }

    /** Any account's history, newest first. */
    @GetMapping("/accounts/{accountId}/transactions")
    @Transactional
    public PageResponse<TransactionView> transactions(@PathVariable Long accountId,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        if (!accountRepository.existsById(accountId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Account not found");
        }
        return PageResponse.from(transactionService.history(accountId, page, size), t -> TransactionView.from(t, accountId));
    }

    private ResponseEntity<TransactionResult> created(Transaction t, Long accountId) {
        Account a = accountRepository.findById(accountId).orElseThrow();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new TransactionResult(TransactionView.from(t, accountId), a.getBalanceCents()));
    }

    private static AdminAccountView toView(Account a) {
        return new AdminAccountView(
            a.getId(),
            a.getOwner().getId(),
            a.getOwner().getUsername(),
            a.getOwner().getEmail(),
            a.getOwner().getRole().name(),
            a.getType(),
            a.getAccountNumber(),
            a.getBalanceCents());
    }
}
