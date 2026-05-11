package com.cloudoptimizer.pso;

public class FitnessModel {
    public double score(
        double utilizationScore,
        double slaScore,
        double energyScore,
        double responseScore,
        double migrationScore,
        double consolidationScore
    ) {
        return 0.24 * utilizationScore
            + 0.22 * slaScore
            + 0.20 * energyScore
            + 0.16 * responseScore
            + 0.10 * migrationScore
            + 0.08 * consolidationScore;
    }
}
