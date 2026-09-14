package dev.dadbank.withdraw;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class WithdrawalDtos {
    private WithdrawalDtos() {}

    public record CreateRequest(
        @Min(value = 1, message = "Amount must be at least 1 cent") long amountCents,
        @Size(max = 140) String note
    ) {}

    public record RejectRequest(@NotBlank @Size(max = 140) String reason) {}

    public record WithdrawalView(
        Long id,
        Long accountId,
        String username,
        String accountNumber,
        long amountCents,
        String note,
        WithdrawalStatus status,
        Instant createdAt,
        String decidedBy,
        Instant decidedAt,
        String rejectionReason
    ) {
        public static WithdrawalView from(Withdrawal w) {
            return new WithdrawalView(
                w.getId(),
                w.getAccount().getId(),
                w.getAccount().getOwner().getUsername(),
                w.getAccount().getAccountNumber(),
                w.getAmountCents(),
                w.getNote(),
                w.getStatus(),
                w.getCreatedAt(),
                w.getDecidedBy() == null ? null : w.getDecidedBy().getUsername(),
                w.getDecidedAt(),
                w.getRejectionReason());
        }
    }

    /** Returned to the admin after a decision: the request plus the account's balance afterwards. */
    public record DecisionResult(WithdrawalView withdrawal, long balanceCents) {}
}
