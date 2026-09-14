package dev.dadbank.transaction;

import dev.dadbank.account.AccountRepository;
import dev.dadbank.account.AccountType;
import dev.dadbank.auth.AuthenticatedUser;
import dev.dadbank.common.ApiException;
import dev.dadbank.common.PageResponse;
import dev.dadbank.transaction.TransactionDtos.TransactionResult;
import dev.dadbank.transaction.TransactionDtos.TransactionView;
import dev.dadbank.transaction.TransactionDtos.TransferRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountRepository accountRepository;

    public TransactionController(TransactionService transactionService, AccountRepository accountRepository) {
        this.transactionService = transactionService;
        this.accountRepository = accountRepository;
    }

    @PostMapping("/transfers")
    @Transactional
    public ResponseEntity<TransactionResult> transfer(@AuthenticationPrincipal AuthenticatedUser principal,
                                                      @Valid @RequestBody TransferRequest req) {
        Transaction t = transactionService.transfer(principal.id(), req.toAccountNumber(), req.amountCents(), req.note());
        var from = accountRepository.findChecking(principal.id()).orElseThrow();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new TransactionResult(TransactionView.from(t, from.getId()), from.getBalanceCents()));
    }

    /** The caller's own history, newest first. {@code account} selects CHECKING (default) or SAVINGS. */
    @GetMapping("/transactions")
    @Transactional
    public PageResponse<TransactionView> myTransactions(@AuthenticationPrincipal AuthenticatedUser principal,
                                                        @RequestParam(name = "account", defaultValue = "CHECKING") AccountType type,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        var account = accountRepository.findByOwnerIdAndType(principal.id(), type)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "You have no such account"));
        return PageResponse.from(transactionService.history(account.getId(), page, size),
            t -> TransactionView.from(t, account.getId()));
    }
}
