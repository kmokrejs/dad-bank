package dev.dadbank.interest;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterestRateRepository extends JpaRepository<InterestRate, Long> {
    Optional<InterestRate> findTopByOrderByCreatedAtDescIdDesc();

    @EntityGraph(attributePaths = "setBy")
    List<InterestRate> findAllByOrderByCreatedAtDescIdDesc();
}
