package dev.dadbank.interest;

import dev.dadbank.user.User;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * One row per rate change; the newest row is the rate in force. Rates are yearly, in basis points
 * (1 bp = 0.01 %, so 250 = 2.5 % p.a.) — integers, never floats.
 */
@Entity
@Table(name = "interest_rates")
public class InterestRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rate_bps", nullable = false)
    private int rateBps;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "set_by_user_id", nullable = false)
    private User setBy;

    @Column(length = 140)
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected InterestRate() {}

    public InterestRate(int rateBps, User setBy, String note) {
        this.rateBps = rateBps;
        this.setBy = setBy;
        this.note = note;
    }

    public Long getId() { return id; }
    public int getRateBps() { return rateBps; }
    public User getSetBy() { return setBy; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}
