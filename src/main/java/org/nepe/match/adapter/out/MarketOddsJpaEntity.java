package org.nepe.match.adapter.out;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * JPA Entity mapping for the {@code market_odds} database table.
 * <p>
 * Stores user-entered Betting Exchange Back and Lay odds per match outcome.
 */
@Entity
@Table(name = "market_odds")
public class MarketOddsJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "match_id", nullable = false)
    private Integer matchId;

    @Column(name = "market_type", nullable = false, length = 30)
    private String marketType;

    @Column(name = "outcome", nullable = false, length = 10)
    private String outcome;

    @Column(name = "back_odds", precision = 6, scale = 3)
    private Double backOdds;

    @Column(name = "lay_odds", precision = 6, scale = 3)
    private Double layOdds;

    /**
     * Default no-args constructor required by JPA / Hibernate.
     */
    protected MarketOddsJpaEntity() {
    }

    /**
     * Constructor for creating a new unpersisted market odds record.
     */
    public MarketOddsJpaEntity(Integer matchId, String marketType, String outcome, Double backOdds, Double layOdds) {
        this(null, matchId, marketType, outcome, backOdds, layOdds);
    }

    /**
     * Full constructor for reconstruction and domain mapping.
     */
    public MarketOddsJpaEntity(Integer id, Integer matchId, String marketType, String outcome, Double backOdds, Double layOdds) {
        this.id = id;
        this.matchId = matchId;
        this.marketType = marketType;
        this.outcome = outcome;
        this.backOdds = backOdds;
        this.layOdds = layOdds;
    }

    // --- Getters and Setters ---

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getMatchId() {
        return matchId;
    }

    public void setMatchId(Integer matchId) {
        this.matchId = matchId;
    }

    public String getMarketType() {
        return marketType;
    }

    public void setMarketType(String marketType) {
        this.marketType = marketType;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public Double getBackOdds() {
        return backOdds;
    }

    public void setBackOdds(Double backOdds) {
        this.backOdds = backOdds;
    }

    public Double getLayOdds() {
        return layOdds;
    }

    public void setLayOdds(Double layOdds) {
        this.layOdds = layOdds;
    }

    // --- Identity, Equality and Diagnostics ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MarketOddsJpaEntity that = (MarketOddsJpaEntity) o;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(matchId, that.matchId) &&
                Objects.equals(marketType, that.marketType) &&
                Objects.equals(outcome, that.outcome);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(matchId, marketType, outcome);
    }

    @Override
    public String toString() {
        return "MarketOddsJpaEntity{" +
                "id=" + id +
                ", matchId=" + matchId +
                ", marketType='" + marketType + '\'' +
                ", outcome='" + outcome + '\'' +
                ", backOdds=" + backOdds +
                ", layOdds=" + layOdds +
                '}';
    }
}
