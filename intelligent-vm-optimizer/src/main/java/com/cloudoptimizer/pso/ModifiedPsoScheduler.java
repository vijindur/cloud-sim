package com.cloudoptimizer.pso;

import java.util.List;

public class ModifiedPsoScheduler extends PsoScheduler {

    public ModifiedPsoScheduler() {
        super(20, 15);
        this.velocityClamp = 0.25;
    }

    @Override
    public String name() {
        return "PSO_MODIFIED";
    }

    @Override
    public int[] schedule(int taskCount, int vmCount, List<Double> vmCapacities) {
        int[] bestSchedule = super.schedule(taskCount, vmCount, vmCapacities);
        double bestFitness = evaluateMapping(bestSchedule, vmCount);

        for (int i = 0; i < maxIterations; i++) {
            int[] candidate = super.schedule(taskCount, vmCount, vmCapacities);
            double candidateFitness = evaluateMapping(candidate, vmCount);
            if (candidateFitness > bestFitness) {
                bestFitness = candidateFitness;
                bestSchedule = candidate;
            }
        }
        return bestSchedule;
    }
}
