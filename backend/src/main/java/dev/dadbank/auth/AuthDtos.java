package dev.dadbank.auth;

import dev.dadbank.account.AccountDto;
import dev.dadbank.user.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 3, max = 32)
        @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "Only letters, digits, '_', '.' and '-' allowed")
        String username,
        @NotBlank @Size(min = 8, max = 128) String password
    ) {}

    public record LoginRequest(
        @NotBlank String login,   // username or email
        @NotBlank String password
    ) {}

    public record UserView(Long id, String email, String username, String role, AccountDto account, AccountDto savings) {
        public static UserView from(User u) {
            return new UserView(u.getId(), u.getEmail(), u.getUsername(), u.getRole().name(),
                u.getAccount() == null ? null : AccountDto.from(u.getAccount()),
                u.getSavings() == null ? null : AccountDto.from(u.getSavings()));
        }
    }

    public record AuthResponse(String token, UserView user) {}
}
