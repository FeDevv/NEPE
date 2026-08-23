package org.nepe.inference.domain;

import org.nepe.shared.exception.DomainValidationException;

/**
 * Pure mathematical implementation of the Dixon & Coles (1997) model for football match prediction.
 * <p>
 * Adjusts the independent Poisson joint probability matrix for low-scoring outcomes
 * ((0,0), (1,0), (0,1), (1,1)) using a competition-specific correlation parameter ({@code rho}).
 * <pre>
 *   P(X=x, Y=y) = tau(x, y; lambda, mu, rho) * Pois(x; lambda) * Pois(y; mu)
 * </pre>
 */
public final class DixonColesModel {

    public static final double DEFAULT_RHO = -0.1200;

    private DixonColesModel() {
        // Pure utility class - prevent instantiation
    }

    /**
     * Calculates the Dixon-Coles adjustment factor tau(x, y) for a specific scoreline.
     *
     * @param x      Home team goals (>= 0)
     * @param y      Away team goals (>= 0)
     * @param lambda Home team expected goals rate (>= 0)
     * @param mu     Away team expected goals rate (>= 0)
     * @param rho    competition dependence coefficient (-1.0 < rho < 1.0)
     * @return non-negative adjustment factor tau(x, y)
     */
    public static double tau(int x, int y, double lambda, double mu, double rho) {
        validateInputs(lambda, mu, rho);
        if (x < 0 || y < 0) {
            throw new DomainValidationException("Goal counts x and y must be non-negative.");
        }

        double factor;
        if (x == 0 && y == 0) {
            factor = 1.0 - (lambda * mu * rho);
        } else if (x == 1 && y == 0) {
            factor = 1.0 + (mu * rho);
        } else if (x == 0 && y == 1) {
            factor = 1.0 + (lambda * rho);
        } else if (x == 1 && y == 1) {
            factor = 1.0 - rho;
        } else {
            factor = 1.0;
        }

        // Ensure non-negativity in extreme parameter configurations
        return Math.max(0.0, factor);
    }

    /**
     * Computes the full Dixon-Coles adjusted joint score probability matrix.
     *
     * @param lambdaHome Home team expected goals rate
     * @param muAway     Away team expected goals rate
     * @param rho        Dixon-Coles correlation parameter
     * @param maxGoals   maximum goal index (inclusive)
     * @return normalized (maxGoals+1) x (maxGoals+1) joint probability matrix
     */
    public static double[][] calculateScoreMatrix(double lambdaHome, double muAway, double rho, int maxGoals) {
        validateInputs(lambdaHome, muAway, rho);

        // 1. Calculate unadjusted independent Poisson distributions
        double[] homeDist = PoissonModel.distribution(lambdaHome, maxGoals);
        double[] awayDist = PoissonModel.distribution(muAway, maxGoals);

        int size = maxGoals + 1;
        double[][] matrix = new double[size][size];
        double sum = 0.0;

        // 2. Populate joint matrix applying tau(x, y) adjustment
        for (int h = 0; h < size; h++) {
            for (int a = 0; a < size; a++) {
                double rawPois = homeDist[h] * awayDist[a];
                double tauFactor = tau(h, a, lambdaHome, muAway, rho);
                matrix[h][a] = rawPois * tauFactor;
                sum += matrix[h][a];
            }
        }

        // 3. Renormalize matrix so that total probability mass equals exactly 1.0
        if (sum > 0.0) {
            for (int h = 0; h < size; h++) {
                for (int a = 0; a < size; a++) {
                    matrix[h][a] /= sum;
                }
            }
        }

        return matrix;
    }

    public static double[][] calculateScoreMatrix(double lambdaHome, double muAway, double rho) {
        return calculateScoreMatrix(lambdaHome, muAway, rho, PoissonModel.DEFAULT_MAX_GOALS);
    }

    public static double[][] calculateScoreMatrix(double lambdaHome, double muAway) {
        return calculateScoreMatrix(lambdaHome, muAway, DEFAULT_RHO, PoissonModel.DEFAULT_MAX_GOALS);
    }

    // --- Market Probability Delegations ---

    public static double calculateHomeWinProbability(double[][] matrix) {
        return PoissonModel.calculateHomeWinProbability(matrix);
    }

    public static double calculateDrawProbability(double[][] matrix) {
        return PoissonModel.calculateDrawProbability(matrix);
    }

    public static double calculateAwayWinProbability(double[][] matrix) {
        return PoissonModel.calculateAwayWinProbability(matrix);
    }

    public static double calculateUnderProbability(double[][] matrix, double threshold) {
        return PoissonModel.calculateUnderProbability(matrix, threshold);
    }

    public static double calculateOverProbability(double[][] matrix, double threshold) {
        return PoissonModel.calculateOverProbability(matrix, threshold);
    }

    public static double calculateBttsYesProbability(double[][] matrix) {
        return PoissonModel.calculateBttsYesProbability(matrix);
    }

    public static double calculateBttsNoProbability(double[][] matrix) {
        return PoissonModel.calculateBttsNoProbability(matrix);
    }

    public static double calculateFairOdds(double probability) {
        return PoissonModel.calculateFairOdds(probability);
    }

    // --- Invariant Validations ---

    private static void validateInputs(double lambda, double mu, double rho) {
        if (Double.isNaN(lambda) || Double.isInfinite(lambda) || lambda < 0.0) {
            throw new DomainValidationException("Lambda (Home expected goals) must be a non-negative finite number.");
        }
        if (Double.isNaN(mu) || Double.isInfinite(mu) || mu < 0.0) {
            throw new DomainValidationException("Mu (Away expected goals) must be a non-negative finite number.");
        }
        if (Double.isNaN(rho) || Double.isInfinite(rho) || rho <= -1.0 || rho >= 1.0) {
            throw new DomainValidationException(
                    String.format("Dixon-Coles rho must be strictly between -1.0 and 1.0 (received: %f).", rho)
            );
        }
    }
}
