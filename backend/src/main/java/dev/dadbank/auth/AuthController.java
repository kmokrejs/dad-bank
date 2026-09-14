package dev.dadbank.auth;

import dev.dadbank.auth.AuthDtos.*;
import dev.dadbank.common.ApiException;
import dev.dadbank.user.Role;
import dev.dadbank.user.User;
import dev.dadbank.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    /** Register a new kid account. Returns a token so the client is logged in immediately. */
    @PostMapping("/auth/register")
    @Transactional
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        User user = userService.register(req.email(), req.username(), req.password(), Role.USER);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new AuthResponse(jwtService.issue(user), UserView.from(user)));
    }

    @PostMapping("/auth/login")
    @Transactional
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        User user = userService.findByLogin(req.login())
            .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
            .orElseThrow(() -> new BadCredentialsException("bad credentials"));
        return new AuthResponse(jwtService.issue(user), UserView.from(user));
    }

    /** Current user + their account (number, balance). */
    @GetMapping("/me")
    @Transactional
    public UserView me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return userService.findById(principal.id())
            .map(UserView::from)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
    }
}
