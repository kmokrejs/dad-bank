package dev.dadbank.transaction;

public enum TransactionType {
    /** Money moved between two accounts. */
    TRANSFER,
    /** Admin put money into an account (e.g. pocket money). No source account. */
    DEPOSIT,
    /** Admin took money out of an account (e.g. bought something). No target account. */
    WITHDRAWAL
}
