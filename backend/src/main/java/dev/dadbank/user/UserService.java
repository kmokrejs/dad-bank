package dev.dadbank.user;

import dev.dadbank.account.Account;
import dev.dadbank.account.AccountNumberGenerator;
import dev.dadbank.account.AccountRepository;
import dev.dadbank.account.AccountType;
import dev.dadbank.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       AccountRepository accountRepository,
                       AccountNumberGenerator accountNumberGenerator,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.accountNumberGenerator = accountNumberGenerator;
        this.passwordEncoder = passwordEncoder;
    }

    /** Registers a user and opens exactly one bank account for them. */
    @Transactional
    public User register(String email, String username, String rawPassword, Role role) {
        String normEmail = email.trim().toLowerCase();
        String normUsername = username.trim();

        if (userRepository.existsByEmailIgnoreCase(normEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
        }
        if (userRepository.existsByUsernameIgnoreCase(normUsername)) {
            throw new ApiException(HttpStatus.CONFLICT, "Username is already taken");
        }

        User user = new User(normEmail, normUsername, passwordEncoder.encode(rawPassword), role);
        user = userRepository.save(user);

        Account account = new Account(user, accountNumberGenerator.generateUnique(), AccountType.CHECKING);
        account = accountRepository.save(account);
        user.addAccount(account);
        return user;
    }

    public boolean existsByUsernameOrEmail(String username, String email) {
        return userRepository.existsByUsernameIgnoreCase(username) || userRepository.existsByEmailIgnoreCase(email);
    }

    /** Login identifier may be either username or email. */
    public Optional<User> findByLogin(String login) {
        String l = login.trim();
        if (l.contains("@")) {
            return userRepository.findByEmailIgnoreCase(l);
        }
        return userRepository.findByUsernameIgnoreCase(l);
    }

    /** Admin-only: overwrite a user's password. */
    @Transactional
    public User resetPassword(Long userId, String newRawPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        return user;
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}
