package dev.dadbank.interest;

import dev.dadbank.common.ApiException;
import dev.dadbank.user.User;
import dev.dadbank.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InterestService {

    /** Sanity cap: 100 % p.a. Enough for a teaching bank, stops fat-finger 25000 %. */
    public static final int MAX_RATE_BPS = 10_000;

    private final InterestRateRepository repository;
    private final UserRepository userRepository;

    public InterestService(InterestRateRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    /** Rate in force now; 0 bps until an admin sets one. */
    public int currentRateBps() {
        return repository.findTopByOrderByCreatedAtDescIdDesc().map(InterestRate::getRateBps).orElse(0);
    }

    @Transactional
    public InterestRate set(Long adminUserId, int rateBps, String note) {
        if (rateBps < 0 || rateBps > MAX_RATE_BPS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Rate must be between 0 and 100 %");
        }
        User admin = userRepository.findById(adminUserId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        String n = note == null || note.isBlank() ? null : note.trim();
        return repository.save(new InterestRate(rateBps, admin, n));
    }

    @Transactional
    public List<InterestRate> history() {
        return repository.findAllByOrderByCreatedAtDescIdDesc();
    }
}
