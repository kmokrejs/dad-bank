package dev.dadbank.account;

public enum AccountType {
    /** Everyday account every user gets at registration. */
    CHECKING,
    /** Optional, at most one per user, earns the bank-wide interest rate. */
    SAVINGS
}
