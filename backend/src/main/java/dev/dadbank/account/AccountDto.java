package dev.dadbank.account;

public record AccountDto(Long id, AccountType type, String accountNumber, long balanceCents) {
    public static AccountDto from(Account a) {
        return new AccountDto(a.getId(), a.getType(), a.getAccountNumber(), a.getBalanceCents());
    }
}
