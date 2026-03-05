package com.cloudoptimizer.pso;

import java.util.List;

public class ModifiedPsoScheduler extends PsoScheduler {

    public ModifiedPsoScheduler() {
        super(20, 15);
    }

    @Override
    public String name() {
        return "PSO_MODIFIED";
    }

    @Override
    public int[] schedule(int taskCount, int vmCount, List<Double> vmCapacities) {
        double threshold = 1e-3;
        double previousBest = Double.NEGATIVE_INFINITY;

        int[] bestSchedule = super.schedule(taskCount, vmCount, vmCapacities);
        for (int i = 0; i < maxIterations; i++) {
            int[] candidate = super.schedule(taskCount, vmCount, vmCapacities);
            double candidateFitness = evaluateFromSchedule(candidate, vmCount);
            if (Math.abs(candidateFitness - previousBest) < threshold) {
                return candidate;
            }
            previousBest = candidateFitness;
            bestSchedule = candidate;
        }
        return bestSchedule;
    }

    private double evaluateFromSchedule(int[] schedule, int vmCount) {
        double utilization = (double) schedule.length / Math.max(1, vmCount * 20);
        double slaCompliance = 1.0 - Math.min(0.25, utilization * 0.1);
        double energyEfficiency = 1.0 - Math.min(0.4, utilization * 0.2);
        return fitnessModel.score(utilization, slaCompliance, energyEfficiency);
    }
}
