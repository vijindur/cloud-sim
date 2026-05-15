package com.cloudoptimizer.scheduler;

import com.cloudoptimizer.core.SimulationConfig;
import com.cloudoptimizer.pso.ModifiedPsoScheduler;
import com.cloudoptimizer.pso.PsoScheduler;

public class SchedulerFactory {
    public SchedulerStrategy fromName(String name) {
        return fromName(name, SimulationConfig.FitnessWeights.defaults());
    }

    public SchedulerStrategy fromName(String name, SimulationConfig.FitnessWeights weights) {
        return switch (name.toUpperCase()) {
            case "ROUND_ROBIN" -> new RoundRobinScheduler();
            case "FCFS" -> new FirstComeFirstServedScheduler();
            case "FIRST_FIT" -> new FirstFitScheduler();
            case "BEST_FIT" -> new BestFitScheduler();
            case "BFD", "BEST_FIT_DECREASING" -> new BestFitDecreasingScheduler();
            case "PSO_STANDARD" -> new PsoScheduler(50, 50, weights);
            case "PSO_MODIFIED" -> new ModifiedPsoScheduler(weights);
            case "HYBRID" -> new HybridScheduler(weights);
            default -> new HybridScheduler(weights);
        };
    }
}
