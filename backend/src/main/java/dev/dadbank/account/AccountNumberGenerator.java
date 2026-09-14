package dev.dadbank.account;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Generates account numbers in the form DB-1234-5678-9012 (kid-readable, easy to type). */
@Component
public class AccountNumberGenerator {

    private static final String PREFIX = "DB";
    private final SecureRandom random = new SecureRandom();
    private final AccountRepository accountRepository;

    public AccountNumberGenerator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public String generateUnique() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = generate();
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique account number");
    }

    String generate() {
        return PREFIX + "-" + block() + "-" + block() + "-" + block();
    }

    private String block() {
        return String.format("%04d", random.nextInt(10_000));
    }
}
