package org.nepe.settings.domain;

import org.nepe.shared.exception.DomainValidationException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Domain entity representing global application and mathematical engine settings.
 * <p>
 * Encapsulates Betfair commission rates, sample sizes for historical regression,
 * inter-season time-decay discount factor (gamma), and live trading profit targets.
 */
public class AppSettings {

    public static final String KEY_COMMISSION_RATE = "commission_rate";
    public static final String KEY_DEFAULT_N_MATCHES = "default_n_matches";
    public static final String KEY_SEASONAL_DECAY_GAMMA = "seasonal_decay_gamma";
    public static final String KEY_GREEN_UP_PROFIT_TARGET = "green_up_profit_target";

    public static final double DEFAULT_COMMISSION_RATE = 0.05; // 5% Betfair standard
    public static final int DEFAULT_N_MATCHES = 10;
    public static final double DEFAULT_SEASONAL_DECAY_GAMMA = 0.70;
    public static final double DEFAULT_GREEN_UP_PROFIT_TARGET = 0.10; // 10%

    private double commissionRate;
    private int defaultNMatches;
    private double seasonalDecayGamma;
    private double greenUpProfitTarget;

    /**
     * Factory method creating settings with default parameters.
     */
    public static AppSettings defaults() {
        return new AppSettings(
                DEFAULT_COMMISSION_RATE,
                DEFAULT_N_MATCHES,
                DEFAULT_SEASONAL_DECAY_GAMMA,
                DEFAULT_GREEN_UP_PROFIT_TARGET
        );
    }

    /**
     * Factory method reconstituting AppSettings from a raw string key-value map (as loaded from the DB).
     * Any missing keys fall back to their respective defaults.
     */
    public static AppSettings fromMap(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return defaults();
        }

        double commission = parseDoubleOrDefault(map.get(KEY_COMMISSION_RATE), DEFAULT_COMMISSION_RATE);
        int nMatches = parseIntOrDefault(map.get(KEY_DEFAULT_N_MATCHES), DEFAULT_N_MATCHES);
        double gamma = parseDoubleOrDefault(map.get(KEY_SEASONAL_DECAY_GAMMA), DEFAULT_SEASONAL_DECAY_GAMMA);
        double target = parseDoubleOrDefault(map.get(KEY_GREEN_UP_PROFIT_TARGET), DEFAULT_GREEN_UP_PROFIT_TARGET);

        return new AppSettings(commission, nMatches, gamma, target);
    }

    /**
     * Full constructor with strict invariant validation.
     */
    public AppSettings(double commissionRate, int defaultNMatches, double seasonalDecayGamma, double greenUpProfitTarget) {
        validateCommissionRate(commissionRate);
        validateDefaultNMatches(defaultNMatches);
        validateSeasonalDecayGamma(seasonalDecayGamma);
        validateGreenUpProfitTarget(greenUpProfitTarget);

        this.commissionRate = commissionRate;
        this.defaultNMatches = defaultNMatches;
        this.seasonalDecayGamma = seasonalDecayGamma;
        this.greenUpProfitTarget = greenUpProfitTarget;
    }

    // --- Domain Business Logic & State Mutations ---

    public void updateCommissionRate(double commissionRate) {
        validateCommissionRate(commissionRate);
        this.commissionRate = commissionRate;
    }

    public void updateDefaultNMatches(int defaultNMatches) {
        validateDefaultNMatches(defaultNMatches);
        this.defaultNMatches = defaultNMatches;
    }

    public void updateSeasonalDecayGamma(double seasonalDecayGamma) {
        validateSeasonalDecayGamma(seasonalDecayGamma);
        this.seasonalDecayGamma = seasonalDecayGamma;
    }

    public void updateGreenUpProfitTarget(double greenUpProfitTarget) {
        validateGreenUpProfitTarget(greenUpProfitTarget);
        this.greenUpProfitTarget = greenUpProfitTarget;
    }

    /**
     * Converts the strongly-typed settings into a key-value map for key-value DB persistence.
     */
    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        map.put(KEY_COMMISSION_RATE, String.valueOf(commissionRate));
        map.put(KEY_DEFAULT_N_MATCHES, String.valueOf(defaultNMatches));
        map.put(KEY_SEASONAL_DECAY_GAMMA, String.valueOf(seasonalDecayGamma));
        map.put(KEY_GREEN_UP_PROFIT_TARGET, String.valueOf(greenUpProfitTarget));
        return Collections.unmodifiableMap(map);
    }

    // --- Invariant Validations ---

    private static void validateCommissionRate(double rate) {
        if (Double.isNaN(rate) || Double.isInfinite(rate)) {
            throw new DomainValidationException("Commission rate must be a valid finite number.");
        }
        if (rate < 0.0 || rate >= 1.0) {
            throw new DomainValidationException(
                    String.format("Commission rate must be in the range [0.0, 1.0) (received: %f).", rate)
            );
        }
    }

    private static void validateDefaultNMatches(int n) {
        if (n < 3 || n > 100) {
            throw new DomainValidationException(
                    String.format("Default match sample size N must be between 3 and 100 (received: %d).", n)
            );
        }
    }

    private static void validateSeasonalDecayGamma(double gamma) {
        if (Double.isNaN(gamma) || Double.isInfinite(gamma)) {
            throw new DomainValidationException("Seasonal decay gamma must be a valid finite number.");
        }
        if (gamma <= 0.0 || gamma > 1.0) {
            throw new DomainValidationException(
                    String.format("Seasonal decay gamma must be in the range (0.0, 1.0] (received: %f).", gamma)
            );
        }
    }

    private static void validateGreenUpProfitTarget(double target) {
        if (Double.isNaN(target) || Double.isInfinite(target)) {
            throw new DomainValidationException("Green-up target must be a valid finite number.");
        }
        if (target <= 0.0 || target > 10.0) {
            throw new DomainValidationException(
                    String.format("Green-up profit target must be in the range (0.0, 10.0] (received: %f).", target)
            );
        }
    }

    // --- Private Parsing Utilities ---

    private static double parseDoubleOrDefault(String value, double defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // --- Getters ---

    public double getCommissionRate() {
        return commissionRate;
    }

    public int getDefaultNMatches() {
        return defaultNMatches;
    }

    public double getSeasonalDecayGamma() {
        return seasonalDecayGamma;
    }

    public double getGreenUpProfitTarget() {
        return greenUpProfitTarget;
    }

    // --- Equality & String Representation ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppSettings that = (AppSettings) o;
        return Double.compare(that.commissionRate, commissionRate) == 0 &&
                defaultNMatches == that.defaultNMatches &&
                Double.compare(that.seasonalDecayGamma, seasonalDecayGamma) == 0 &&
                Double.compare(that.greenUpProfitTarget, greenUpProfitTarget) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(commissionRate, defaultNMatches, seasonalDecayGamma, greenUpProfitTarget);
    }

    @Override
    public String toString() {
        return "AppSettings{" +
                "commissionRate=" + commissionRate +
                ", defaultNMatches=" + defaultNMatches +
                ", seasonalDecayGamma=" + seasonalDecayGamma +
                ", greenUpProfitTarget=" + greenUpProfitTarget +
                '}';
    }
}
