package dev.dadbank.withdraw;

import dev.dadbank.auth.AuthenticatedUser;
import dev.dadbank.common.PageResponse;
import dev.dadbank.withdraw.WithdrawalDtos.CreateRequest;
import dev.dadbank.withdraw.WithdrawalDtos.DecisionResult;
import dev.dadbank.withdraw.WithdrawalDtos.RejectRequest;
import dev.dadbank.withdraw.WithdrawalDtos.WithdrawalView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    public WithdrawalController(WithdrawalService withdrawalService) {
        this.withdrawalService = withdrawalService;
    }

    /** Kid: ask for cash out of the main account. Starts PENDING until the admin decides. */
    @PostMapping("/withdrawals")
    @Transactional
    public ResponseEntity<WithdrawalView> create(@AuthenticationPrincipal AuthenticatedUser principal,
                                                 @Valid @RequestBody CreateRequest req) {
        Withdrawal w = withdrawalService.request(principal.id(), req.amountCents(), req.note());
        return ResponseEntity.status(HttpStatus.CREATED).body(WithdrawalView.from(w));
    }

    /** Kid: own requests, every status, newest first. */
    @GetMapping("/withdrawals")
    @Transactional
    public PageResponse<WithdrawalView> mine(@AuthenticationPrincipal AuthenticatedUser principal,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(withdrawalService.myRequests(principal.id(), page, size), WithdrawalView::from);
    }

    /** Admin: the queue of pending requests across all kids, oldest first. */
    @GetMapping("/admin/withdrawals/pending")
    @Transactional
    public PageResponse<WithdrawalView> pending(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(withdrawalService.pendingQueue(page, size), WithdrawalView::from);
    }

    @PostMapping("/admin/withdrawals/{id}/approve")
    @Transactional
    public DecisionResult approve(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long id) {
        Withdrawal w = withdrawalService.approve(principal.id(), id);
        return new DecisionResult(WithdrawalView.from(w), w.getAccount().getBalanceCents());
    }

    @PostMapping("/admin/withdrawals/{id}/reject")
    @Transactional
    public DecisionResult reject(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long id,
                                 @Valid @RequestBody RejectRequest req) {
        Withdrawal w = withdrawalService.reject(principal.id(), id, req.reason());
        return new DecisionResult(WithdrawalView.from(w), w.getAccount().getBalanceCents());
    }
}
