package com.cloudoptimizer.pso;

import com.cloudoptimizer.core.SimulationConfig;

public class FitnessModel {
    private final SimulationConfig.FitnessWeights weights;

    public FitnessModel() {
        this(SimulationConfig.FitnessWeights.defaults());
    }

    public FitnessModel(SimulationConfig.FitnessWeights weights) {
        this.weights = weights != null ? weights : SimulationConfig.FitnessWeights.defaults();
    }

    public double score(
        double utilizationScore,
        double slaScore,
        double energyScore,
        double responseScore,
        double migrationScore,
        double consolidationScore
    ) {
        double totalWeight = weights.utilization() + weights.sla() + weights.energy()
            + weights.responseTime() + weights.migration() + weights.consolidation();
        double safeTotal = totalWeight > 0 ? totalWeight : 1.0;
        return (weights.utilization() * utilizationScore
            + weights.sla() * slaScore
            + weights.energy() * energyScore
            + weights.responseTime() * responseScore
            + weights.migration() * migrationScore
            + weights.consolidation() * consolidationScore) / safeTotal;
    }
}
