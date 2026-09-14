package dev.dadbank.withdraw;

public enum WithdrawalStatus {
    /** Waiting for the admin to decide. Balance is untouched. */
    PENDING,
    /** Admin approved: money left the account and a WITHDRAWAL ledger row exists. */
    APPROVED,
    /** Admin rejected with a reason. Balance untouched. */
    REJECTED
}
