package dev.dadbank.user;

import dev.dadbank.account.Account;
import dev.dadbank.account.AccountType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Account> accounts = new ArrayList<>();

    protected User() {}

    public User(String email, String username, String passwordHash, Role role) {
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
    public List<Account> getAccounts() { return accounts; }

    public void addAccount(Account account) { accounts.add(account); }

    private Optional<Account> accountOf(AccountType type) {
        return accounts.stream().filter(a -> a.getType() == type).findFirst();
    }

    /** The everyday account; every user has exactly one. */
    public Account getAccount() { return accountOf(AccountType.CHECKING).orElse(null); }

    /** The optional savings account, or null. */
    public Account getSavings() { return accountOf(AccountType.SAVINGS).orElse(null); }
}
