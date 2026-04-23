package com.cloudoptimizer.pso;

import com.cloudoptimizer.scheduler.SchedulerStrategy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class BaseMetaHeuristicScheduler implements SchedulerStrategy {
    protected final FitnessModel fitnessModel = new FitnessModel();
    protected List<Double> lastConvergence = new ArrayList<>();

    protected double evaluateMapping(int[] mapping, int vmCount) {
        double[] vmLoad = new double[Math.max(vmCount, 1)];
        for (int vm : mapping) {
            vmLoad[Math.max(0, Math.min(vm, vmLoad.length - 1))] += 1.0;
        }

        double mean = Arrays.stream(vmLoad).average().orElse(0.0);
        double variance = 0.0;
        int active = 0;
        for (double load : vmLoad) {
            variance += Math.pow(load - mean, 2);
            if (load > 0) {
                active++;
            }
        }
        variance /= vmLoad.length;
        double normalizedVariance = variance / Math.max(1.0, mean * mean + 1e-9);

        double utilization = active / (double) vmLoad.length;
        double slaCompliance = Math.max(0.0, 1.0 - Math.min(0.8, normalizedVariance * 0.35));
        double energyEfficiency = Math.max(0.0, 1.0 - utilization);
        return fitnessModel.score(1 - utilization, slaCompliance, energyEfficiency);
    }

    public List<Double> lastConvergenceCurve() {
        return lastConvergence;
    }
}
