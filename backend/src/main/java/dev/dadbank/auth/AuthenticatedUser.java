package dev.dadbank.auth;

import dev.dadbank.user.Role;

/** Lightweight principal stored in the SecurityContext (no entity / lazy proxies). */
public record AuthenticatedUser(Long id, String username, Role role) {}
