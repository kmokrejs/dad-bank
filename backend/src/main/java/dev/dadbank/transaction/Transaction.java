package dev.dadbank.transaction;

import dev.dadbank.account.Account;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * One ledger row. For TRANSFER both accounts are set; DEPOSIT has only {@code to};
 * WITHDRAWAL has only {@code from}. Amount is always positive, in cents.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id")
    private Account fromAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id")
    private Account toAccount;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(length = 140)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Transaction() {}

    public Transaction(TransactionType type, Account fromAccount, Account toAccount, long amountCents, String note) {
        this.type = type;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amountCents = amountCents;
        this.note = note;
    }

    public Long getId() { return id; }
    public TransactionType getType() { return type; }
    public Account getFromAccount() { return fromAccount; }
    public Account getToAccount() { return toAccount; }
    public long getAmountCents() { return amountCents; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}
