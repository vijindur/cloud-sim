package com.cloudoptimizer.pso;

public class FitnessModel {
    public double score(double utilization, double slaCompliance, double energyEfficiency) {
        return 0.40 * utilization + 0.35 * slaCompliance + 0.25 * energyEfficiency;
    }
}
