package dev.dadbank.withdraw;

import dev.dadbank.account.Account;
import dev.dadbank.transaction.Transaction;
import dev.dadbank.user.User;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * A kid's request to take cash out of their main account. Lives in its own table because it has a
 * lifecycle (PENDING → APPROVED/REJECTED); the ledger only gets a row once it is approved.
 */
@Entity
@Table(name = "withdrawals")
public class Withdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(length = 140)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WithdrawalStatus status = WithdrawalStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    /** Who approved/rejected and when; null while PENDING. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_user_id")
    private User decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "rejection_reason", length = 140)
    private String rejectionReason;

    /** The ledger row created on approval; null otherwise. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    protected Withdrawal() {}

    public Withdrawal(Account account, long amountCents, String note) {
        this.account = account;
        this.amountCents = amountCents;
        this.note = note;
    }

    public void approve(User admin, Transaction ledgerRow) {
        this.status = WithdrawalStatus.APPROVED;
        this.decidedBy = admin;
        this.decidedAt = Instant.now();
        this.transaction = ledgerRow;
    }

    public void reject(User admin, String reason) {
        this.status = WithdrawalStatus.REJECTED;
        this.decidedBy = admin;
        this.decidedAt = Instant.now();
        this.rejectionReason = reason;
    }

    public boolean isPending() { return status == WithdrawalStatus.PENDING; }

    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public long getAmountCents() { return amountCents; }
    public String getNote() { return note; }
    public WithdrawalStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public User getDecidedBy() { return decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public Transaction getTransaction() { return transaction; }
}
