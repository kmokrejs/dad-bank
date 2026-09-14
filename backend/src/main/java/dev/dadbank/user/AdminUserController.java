package dev.dadbank.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** God-mode user management. Access is restricted to ROLE_ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    public record ResetPasswordRequest(@NotBlank @Size(min = 8, max = 128) String newPassword) {}

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    /** Set a new password for a user (the kid forgot theirs). Existing JWTs stay valid until they expire. */
    @PostMapping("/{userId}/password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long userId, @Valid @RequestBody ResetPasswordRequest req) {
        userService.resetPassword(userId, req.newPassword());
        return ResponseEntity.noContent().build();
    }
}
