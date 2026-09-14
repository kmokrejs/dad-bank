package dev.dadbank.savings;

import dev.dadbank.account.Account;
import dev.dadbank.account.AccountDto;
import dev.dadbank.account.AccountRepository;
import dev.dadbank.auth.AuthenticatedUser;
import dev.dadbank.transaction.Transaction;
import dev.dadbank.transaction.TransactionDtos.AdjustRequest;
import dev.dadbank.transaction.TransactionDtos.TransactionView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/savings")
public class SavingsController {

    /** After a move: the transaction (from the savings account's point of view) and both new balances. */
    public record MoveResult(TransactionView transaction, long checkingBalanceCents, long savingsBalanceCents) {}

    private final SavingsService savingsService;
    private final AccountRepository accountRepository;

    public SavingsController(SavingsService savingsService, AccountRepository accountRepository) {
        this.savingsService = savingsService;
        this.accountRepository = accountRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<AccountDto> open(@AuthenticationPrincipal AuthenticatedUser principal) {
        Account a = savingsService.open(principal.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountDto.from(a));
    }

    /** Move money from the main account into savings. */
    @PostMapping("/deposit")
    @Transactional
    public ResponseEntity<MoveResult> deposit(@AuthenticationPrincipal AuthenticatedUser principal, @Valid @RequestBody AdjustRequest req) {
        Transaction t = savingsService.deposit(principal.id(), req.amountCents(), req.note());
        return result(principal.id(), t);
    }

    /** Move money from savings back to the main account. */
    @PostMapping("/withdraw")
    @Transactional
    public ResponseEntity<MoveResult> withdraw(@AuthenticationPrincipal AuthenticatedUser principal, @Valid @RequestBody AdjustRequest req) {
        Transaction t = savingsService.withdraw(principal.id(), req.amountCents(), req.note());
        return result(principal.id(), t);
    }

    private ResponseEntity<MoveResult> result(Long userId, Transaction t) {
        Account checking = accountRepository.findChecking(userId).orElseThrow();
        Account savings = accountRepository.findSavings(userId).orElseThrow();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new MoveResult(TransactionView.from(t, savings.getId()), checking.getBalanceCents(), savings.getBalanceCents()));
    }
}
