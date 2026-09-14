package dev.dadbank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "dadbank")
public record DadBankProperties(Jwt jwt, Admin admin, Cors cors) {
    public record Jwt(String secret, long expirationMinutes) {}
    public record Admin(String email, String username, String password) {}
    public record Cors(List<String> allowedOrigins) {}
}
