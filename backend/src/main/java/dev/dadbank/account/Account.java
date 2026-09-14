package dev.dadbank.account;

import dev.dadbank.user.User;
import jakarta.persistence.*;

import java.time.Instant;

/** One user has exactly one CHECKING account and at most one SAVINGS account (enforced by the unique (user_id, type) pair). */
@Entity
@Table(name = "accounts", uniqueConstraints = @UniqueConstraint(name = "uk_accounts_user_type", columnNames = {"user_id", "type"}))
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountType type = AccountType.CHECKING;

    @Column(name = "account_number", nullable = false, unique = true, length = 32)
    private String accountNumber;

    /** Money is always stored as integer cents. */
    @Column(name = "balance_cents", nullable = false)
    private long balanceCents = 0L;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Account() {}

    public Account(User owner, String accountNumber, AccountType type) {
        this.owner = owner;
        this.accountNumber = accountNumber;
        this.type = type;
    }

    public Long getId() { return id; }
    public User getOwner() { return owner; }
    public AccountType getType() { return type; }
    public String getAccountNumber() { return accountNumber; }
    public long getBalanceCents() { return balanceCents; }
    public Instant getCreatedAt() { return createdAt; }

    public void credit(long cents) {
        if (cents <= 0) throw new IllegalArgumentException("credit amount must be positive");
        this.balanceCents += cents;
    }

    public void debit(long cents) {
        if (cents <= 0) throw new IllegalArgumentException("debit amount must be positive");
        if (this.balanceCents < cents) throw new IllegalStateException("insufficient funds");
        this.balanceCents -= cents;
    }
}
