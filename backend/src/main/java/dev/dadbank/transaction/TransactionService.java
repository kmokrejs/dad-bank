package dev.dadbank.transaction;

import dev.dadbank.account.Account;
import dev.dadbank.account.AccountRepository;
import dev.dadbank.common.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /** Move money from the caller's account to another account. Atomic: both balances and the ledger row commit together. */
    @Transactional
    public Transaction transfer(Long fromUserId, String toAccountNumber, long amountCents, String note) {
        Account from = accountRepository.findChecking(fromUserId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "You have no account"));
        Account to = accountRepository.findByAccountNumber(normalize(toAccountNumber))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recipient account not found"));
        return move(from, to, amountCents, note);
    }

    /** Move money between two accounts (any owners). Atomic: both balances and the ledger row commit together. */
    @Transactional
    public Transaction move(Account from, Account to, long amountCents, String note) {
        requirePositive(amountCents);
        if (from.getId().equals(to.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot send money to yourself");
        }
        if (from.getBalanceCents() < amountCents) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient funds");
        }
        from.debit(amountCents);
        to.credit(amountCents);
        return transactionRepository.save(new Transaction(TransactionType.TRANSFER, from, to, amountCents, clean(note)));
    }

    /** Admin puts money into an account (pocket money, gift, ...). */
    @Transactional
    public Transaction deposit(Long accountId, long amountCents, String note) {
        requirePositive(amountCents);
        Account to = requireAccount(accountId);
        to.credit(amountCents);
        return transactionRepository.save(new Transaction(TransactionType.DEPOSIT, null, to, amountCents, clean(note)));
    }

    /** Admin takes money out of an account (the kid bought something in the real world). */
    @Transactional
    public Transaction withdraw(Long accountId, long amountCents, String note) {
        requirePositive(amountCents);
        Account from = requireAccount(accountId);
        if (from.getBalanceCents() < amountCents) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient funds");
        }
        from.debit(amountCents);
        return transactionRepository.save(new Transaction(TransactionType.WITHDRAWAL, from, null, amountCents, clean(note)));
    }

    public static final int MAX_PAGE_SIZE = 100;

    /** Ledger rows touching the given account, newest first. */
    @Transactional
    public Page<Transaction> history(Long accountId, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        return transactionRepository.findByAccountId(accountId, PageRequest.of(Math.max(0, page), safeSize));
    }

    private Account requireAccount(Long accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    private static void requirePositive(long amountCents) {
        if (amountCents <= 0) throw new ApiException(HttpStatus.BAD_REQUEST, "Amount must be positive");
    }

    private static String normalize(String accountNumber) {
        return accountNumber.trim().toUpperCase();
    }

    private static String clean(String note) {
        if (note == null) return null;
        String n = note.trim();
        return n.isEmpty() ? null : n;
    }
}
