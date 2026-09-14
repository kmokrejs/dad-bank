package dev.dadbank.interest;

import dev.dadbank.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
public class InterestController {

    public record CurrentRate(int rateBps) {}

    public record SetRateRequest(
        @Min(0) @Max(InterestService.MAX_RATE_BPS) int rateBps,
        @Size(max = 140) String note
    ) {}

    public record RateChange(Long id, int rateBps, String setBy, String note, Instant createdAt) {
        static RateChange from(InterestRate r) {
            return new RateChange(r.getId(), r.getRateBps(), r.getSetBy().getUsername(), r.getNote(), r.getCreatedAt());
        }
    }

    private final InterestService interestService;

    public InterestController(InterestService interestService) {
        this.interestService = interestService;
    }

    /** Any signed-in user: the rate savings accounts earn right now. */
    @GetMapping("/interest")
    public CurrentRate current() {
        return new CurrentRate(interestService.currentRateBps());
    }

    /** Admin: every rate change, newest first (feeds the future graph). */
    @GetMapping("/admin/interest")
    @Transactional
    public List<RateChange> history() {
        return interestService.history().stream().map(RateChange::from).toList();
    }

    /** Admin: set a new bank-wide rate. */
    @PostMapping("/admin/interest")
    @Transactional
    public ResponseEntity<RateChange> set(@AuthenticationPrincipal AuthenticatedUser principal, @Valid @RequestBody SetRateRequest req) {
        InterestRate r = interestService.set(principal.id(), req.rateBps(), req.note());
        return ResponseEntity.status(HttpStatus.CREATED).body(RateChange.from(r));
    }
}
