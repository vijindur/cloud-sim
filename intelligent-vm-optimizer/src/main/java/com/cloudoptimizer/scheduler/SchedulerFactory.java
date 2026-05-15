package com.cloudoptimizer.scheduler;

import com.cloudoptimizer.pso.ModifiedPsoScheduler;
import com.cloudoptimizer.pso.PsoScheduler;

public class SchedulerFactory {
    public SchedulerStrategy fromName(String name) {
        return switch (name.toUpperCase()) {
            case "ROUND_ROBIN" -> new RoundRobinScheduler();
            case "FCFS" -> new FirstComeFirstServedScheduler();
            case "FIRST_FIT" -> new FirstFitScheduler();
            case "BEST_FIT" -> new BestFitScheduler();
            case "BFD", "BEST_FIT_DECREASING" -> new BestFitDecreasingScheduler();
            case "PSO_STANDARD" -> new PsoScheduler(50, 50);
            case "PSO_MODIFIED" -> new ModifiedPsoScheduler();
            case "HYBRID" -> new HybridScheduler();
            default -> new HybridScheduler();
        };
    }
}
