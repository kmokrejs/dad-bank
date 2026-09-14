package dev.dadbank.transaction;

import dev.dadbank.account.AccountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class TransactionDtos {
    private TransactionDtos() {}

    public record TransferRequest(
        @NotBlank String toAccountNumber,
        @Min(value = 1, message = "Amount must be at least 1 cent") long amountCents,
        @Size(max = 140) String note
    ) {}

    /** Used by admin deposit / withdrawal. */
    public record AdjustRequest(
        @Min(value = 1, message = "Amount must be at least 1 cent") long amountCents,
        @Size(max = 140) String note
    ) {}

    public record PartyView(String accountNumber, String username, AccountType accountType) {}

    /**
     * A transaction as seen from one account's point of view:
     * {@code direction} is IN or OUT and {@code counterparty} is the other side (null for deposits/withdrawals).
     */
    public record TransactionView(
        Long id,
        TransactionType type,
        String direction,
        long amountCents,
        String note,
        PartyView counterparty,
        Instant createdAt
    ) {
        public static TransactionView from(Transaction t, Long viewpointAccountId) {
            boolean incoming = t.getToAccount() != null && t.getToAccount().getId().equals(viewpointAccountId);
            var other = incoming ? t.getFromAccount() : t.getToAccount();
            PartyView counterparty = other == null ? null
                : new PartyView(other.getAccountNumber(), other.getOwner().getUsername(), other.getType());
            return new TransactionView(t.getId(), t.getType(), incoming ? "IN" : "OUT",
                t.getAmountCents(), t.getNote(), counterparty, t.getCreatedAt());
        }
    }

    /** Returned after a transfer/deposit: the new balance of the account the caller cares about. */
    public record TransactionResult(TransactionView transaction, long balanceCents) {}
}
