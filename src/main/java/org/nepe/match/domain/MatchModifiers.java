package org.nepe.match.domain;

import org.nepe.shared.exception.DomainValidationException;

import java.util.Objects;

/**
 * Value Object encapsulating pre-match context modifiers and tactical multipliers.
 * <p>
 * Includes home/away attack & defense injury/lineup multipliers, Must-Win motivation flags,
 * Low-Urgency draw-satisfaction flags, and neutral venue settings.
 */
public class MatchModifiers {

    public static final double DEFAULT_MULTIPLIER = 1.00;
    public static final double MIN_MULTIPLIER = 0.10;
    public static final double MAX_MULTIPLIER = 3.00;

    private boolean neutralVenue;
    private boolean mustWinHome;
    private boolean mustWinAway;
    private boolean lowUrgencyHome;
    private boolean lowUrgencyAway;
    private double modAttHome;
    private double modDefHome;
    private double modAttAway;
    private double modDefAway;

    /**
     * Factory method creating standard unadjusted default modifiers.
     */
    public static MatchModifiers defaultModifiers() {
        return new MatchModifiers(
                false,
                false,
                false,
                false,
                false,
                DEFAULT_MULTIPLIER,
                DEFAULT_MULTIPLIER,
                DEFAULT_MULTIPLIER,
                DEFAULT_MULTIPLIER
        );
    }

    /**
     * Full constructor with invariant validations.
     */
    public MatchModifiers(boolean neutralVenue,
                          boolean mustWinHome,
                          boolean mustWinAway,
                          boolean lowUrgencyHome,
                          boolean lowUrgencyAway,
                          double modAttHome,
                          double modDefHome,
                          double modAttAway,
                          double modDefAway) {
        validateMultipliers(modAttHome, modDefHome, modAttAway, modDefAway);
        validateContradictions(mustWinHome, lowUrgencyHome, "Home");
        validateContradictions(mustWinAway, lowUrgencyAway, "Away");

        this.neutralVenue = neutralVenue;
        this.mustWinHome = mustWinHome;
        this.mustWinAway = mustWinAway;
        this.lowUrgencyHome = lowUrgencyHome;
        this.lowUrgencyAway = lowUrgencyAway;
        this.modAttHome = modAttHome;
        this.modDefHome = modDefHome;
        this.modAttAway = modAttAway;
        this.modDefAway = modDefAway;
    }

    // --- Domain Queries ---

    /**
     * Checks if both teams share low urgency (e.g. mutual agreement on a draw).
     */
    public boolean isMutualLowUrgency() {
        return lowUrgencyHome && lowUrgencyAway;
    }

    /**
     * Checks if any non-standard modifier is active.
     */
    public boolean hasCustomModifiers() {
        return neutralVenue || mustWinHome || mustWinAway || lowUrgencyHome || lowUrgencyAway ||
                modAttHome != DEFAULT_MULTIPLIER || modDefHome != DEFAULT_MULTIPLIER ||
                modAttAway != DEFAULT_MULTIPLIER || modDefAway != DEFAULT_MULTIPLIER;
    }

    // --- Invariant Validations ---

    private static void validateMultipliers(double attH, double defH, double attA, double defA) {
        validateSingleMultiplier("Home Attack", attH);
        validateSingleMultiplier("Home Defense", defH);
        validateSingleMultiplier("Away Attack", attA);
        validateSingleMultiplier("Away Defense", defA);
    }

    private static void validateSingleMultiplier(String label, double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new DomainValidationException(label + " modifier must be a valid finite number.");
        }
        if (value < MIN_MULTIPLIER || value > MAX_MULTIPLIER) {
            throw new DomainValidationException(
                    String.format("%s modifier must be between %.2f and %.2f (received: %.2f).",
                            label, MIN_MULTIPLIER, MAX_MULTIPLIER, value)
            );
        }
    }

    private static void validateContradictions(boolean mustWin, boolean lowUrgency, String side) {
        if (mustWin && lowUrgency) {
            throw new DomainValidationException(
                    String.format("%s team cannot be simultaneously marked as Must-Win and Low-Urgency.", side)
            );
        }
    }

    // --- Getters ---

    public boolean isNeutralVenue() {
        return neutralVenue;
    }

    public boolean isMustWinHome() {
        return mustWinHome;
    }

    public boolean isMustWinAway() {
        return mustWinAway;
    }

    public boolean isLowUrgencyHome() {
        return lowUrgencyHome;
    }

    public boolean isLowUrgencyAway() {
        return lowUrgencyAway;
    }

    public double getModAttHome() {
        return modAttHome;
    }

    public double getModDefHome() {
        return modDefHome;
    }

    public double getModAttAway() {
        return modAttAway;
    }

    public double getModDefAway() {
        return modDefAway;
    }

    // --- Equality & Identity ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MatchModifiers that = (MatchModifiers) o;
        return neutralVenue == that.neutralVenue &&
                mustWinHome == that.mustWinHome &&
                mustWinAway == that.mustWinAway &&
                lowUrgencyHome == that.lowUrgencyHome &&
                lowUrgencyAway == that.lowUrgencyAway &&
                Double.compare(that.modAttHome, modAttHome) == 0 &&
                Double.compare(that.modDefHome, modDefHome) == 0 &&
                Double.compare(that.modAttAway, modAttAway) == 0 &&
                Double.compare(that.modDefAway, modDefAway) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(neutralVenue, mustWinHome, mustWinAway, lowUrgencyHome, lowUrgencyAway,
                modAttHome, modDefHome, modAttAway, modDefAway);
    }

    @Override
    public String toString() {
        return "MatchModifiers{" +
                "neutralVenue=" + neutralVenue +
                ", mustWinHome=" + mustWinHome +
                ", mustWinAway=" + mustWinAway +
                ", lowUrgencyHome=" + lowUrgencyHome +
                ", lowUrgencyAway=" + lowUrgencyAway +
                ", modAttHome=" + modAttHome +
                ", modDefHome=" + modDefHome +
                ", modAttAway=" + modAttAway +
                ", modDefAway=" + modDefAway +
                '}';
    }
}
