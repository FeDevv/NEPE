package org.nepe.inference.domain;

import org.nepe.shared.exception.DomainValidationException;

/**
 * Pure mathematical implementation of the Poisson probability distribution for football modeling.
 * <p>
 * Computes univariate Poisson probabilities, bivariate joint scoreline matrices,
 * and aggregate market probabilities (1X2, Under/Over 0.5-4.5, BTTS).
 */
public final class PoissonModel {

    public static final int DEFAULT_MAX_GOALS = 9; // Produces a 10x10 matrix (scores 0 to 9)

    private PoissonModel() {
        // Pure utility class - prevent instantiation
    }

    /**
     * Calculates the single Poisson probability P(X = k) given rate parameter lambda:
     * <pre>
     *   P(X = k) = (lambda^k * e^(-lambda)) / k!
     * </pre>
     *
     * @param lambda average expected goal rate (must be >= 0)
     * @param k      number of goals (must be >= 0)
     * @return probability P(X = k) in range [0.0, 1.0]
     */
    public static double probability(double lambda, int k) {
        validateLambda(lambda);
        if (k < 0) {
            throw new DomainValidationException("Goal count k cannot be negative (received: " + k + ").");
        }
        if (lambda == 0.0) {
            return (k == 0) ? 1.0 : 0.0;
        }

        // Numerically stable iterative calculation avoiding factorial overflow
        double prob = Math.exp(-lambda);
        for (int i = 1; i <= k; i++) {
            prob *= (lambda / i);
        }
        return prob;
    }

    /**
     * Computes the 1D Poisson probability distribution array for goals from 0 up to maxGoals.
     *
     * @param lambda   average expected goal rate
     * @param maxGoals maximum goal index to compute (inclusive)
     * @return array where index k holds P(X = k)
     */
    public static double[] distribution(double lambda, int maxGoals) {
        validateLambda(lambda);
        validateMaxGoals(maxGoals);

        double[] dist = new double[maxGoals + 1];
        if (lambda == 0.0) {
            dist[0] = 1.0;
            return dist;
        }

        dist[0] = Math.exp(-lambda);
        for (int k = 1; k <= maxGoals; k++) {
            dist[k] = dist[k - 1] * (lambda / k);
        }
        return dist;
    }

    /**
     * Generates a 2D joint score matrix under the standard independent Poisson assumption:
     * <pre>
     *   M[homeGoals][awayGoals] = P(Home = homeGoals) * P(Away = awayGoals)
     * </pre>
     *
     * @param lambdaHome expected goals for Home team
     * @param muAway     expected goals for Away team
     * @param maxGoals   maximum goal index (inclusive)
     * @return normalized (maxGoals+1) x (maxGoals+1) joint probability matrix
     */
    public static double[][] calculateScoreMatrix(double lambdaHome, double muAway, int maxGoals) {
        double[] homeDist = distribution(lambdaHome, maxGoals);
        double[] awayDist = distribution(muAway, maxGoals);

        int size = maxGoals + 1;
        double[][] matrix = new double[size][size];
        double sum = 0.0;

        for (int h = 0; h < size; h++) {
            for (int a = 0; a < size; a++) {
                matrix[h][a] = homeDist[h] * awayDist[a];
                sum += matrix[h][a];
            }
        }

        // Normalize matrix to ensure sum = 1.0 (compensating for tail truncation at maxGoals)
        if (sum > 0.0 && Math.abs(sum - 1.0) > 1e-9) {
            for (int h = 0; h < size; h++) {
                for (int a = 0; a < size; a++) {
                    matrix[h][a] /= sum;
                }
            }
        }

        return matrix;
    }

    public static double[][] calculateScoreMatrix(double lambdaHome, double muAway) {
        return calculateScoreMatrix(lambdaHome, muAway, DEFAULT_MAX_GOALS);
    }

    // --- Market Aggregations from Score Matrix ---

    /**
     * Calculates the Home Win probability (P(1)) from a joint score matrix.
     */
    public static double calculateHomeWinProbability(double[][] matrix) {
        validateMatrix(matrix);
        double p = 0.0;
        for (int h = 0; h < matrix.length; h++) {
            for (int a = 0; a < matrix[h].length; a++) {
                if (h > a) {
                    p += matrix[h][a];
                }
            }
        }
        return clampProbability(p);
    }

    /**
     * Calculates the Draw probability (P(X)) from a joint score matrix.
     */
    public static double calculateDrawProbability(double[][] matrix) {
        validateMatrix(matrix);
        double p = 0.0;
        int minDim = Math.min(matrix.length, matrix[0].length);
        for (int i = 0; i < minDim; i++) {
            p += matrix[i][i];
        }
        return clampProbability(p);
    }

    /**
     * Calculates the Away Win probability (P(2)) from a joint score matrix.
     */
    public static double calculateAwayWinProbability(double[][] matrix) {
        validateMatrix(matrix);
        double p = 0.0;
        for (int h = 0; h < matrix.length; h++) {
            for (int a = 0; a < matrix[h].length; a++) {
                if (h < a) {
                    p += matrix[h][a];
                }
            }
        }
        return clampProbability(p);
    }

    /**
     * Calculates the Under X.5 Goals probability.
     */
    public static double calculateUnderProbability(double[][] matrix, double threshold) {
        validateMatrix(matrix);
        if (threshold < 0.0) {
            throw new DomainValidationException("Goal threshold must be non-negative.");
        }
        double p = 0.0;
        for (int h = 0; h < matrix.length; h++) {
            for (int a = 0; a < matrix[h].length; a++) {
                if ((h + a) < threshold) {
                    p += matrix[h][a];
                }
            }
        }
        return clampProbability(p);
    }

    /**
     * Calculates the Over X.5 Goals probability.
     */
    public static double calculateOverProbability(double[][] matrix, double threshold) {
        return clampProbability(1.0 - calculateUnderProbability(matrix, threshold));
    }

    /**
     * Calculates Both Teams to Score (BTTS Yes) probability.
     */
    public static double calculateBttsYesProbability(double[][] matrix) {
        validateMatrix(matrix);
        double p = 0.0;
        for (int h = 1; h < matrix.length; h++) {
            for (int a = 1; a < matrix[h].length; a++) {
                p += matrix[h][a];
            }
        }
        return clampProbability(p);
    }

    /**
     * Calculates Both Teams to Score (BTTS No) probability.
     */
    public static double calculateBttsNoProbability(double[][] matrix) {
        return clampProbability(1.0 - calculateBttsYesProbability(matrix));
    }

    /**
     * Computes the theoretical fair odds from pure probability: 1.0 / P.
     */
    public static double calculateFairOdds(double probability) {
        if (probability <= 0.0) {
            return 1000.0; // Max odds ceiling
        }
        if (probability >= 1.0) {
            return 1.00;
        }
        return 1.0 / probability;
    }

    // --- Invariant Validations ---

    private static void validateLambda(double lambda) {
        if (Double.isNaN(lambda) || Double.isInfinite(lambda)) {
            throw new DomainValidationException("Poisson rate parameter (lambda/mu) must be a valid finite number.");
        }
        if (lambda < 0.0) {
            throw new DomainValidationException("Poisson rate parameter (lambda/mu) cannot be negative (received: " + lambda + ").");
        }
    }

    private static void validateMaxGoals(int maxGoals) {
        if (maxGoals < 1 || maxGoals > 30) {
            throw new DomainValidationException("maxGoals must be between 1 and 30 (received: " + maxGoals + ").");
        }
    }

    private static void validateMatrix(double[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            throw new DomainValidationException("Score matrix cannot be null or empty.");
        }
    }

    private static double clampProbability(double p) {
        if (p < 0.0) return 0.0;
        if (p > 1.0) return 1.0;
        return p;
    }
}
