package com.cloudoptimizer.scheduler;

import com.cloudoptimizer.decision.AdaptiveDecisionEngine;
import com.cloudoptimizer.pso.ModifiedPsoScheduler;
import com.cloudoptimizer.workload.WorkloadAnalyzer.WorkloadProfile;
import java.util.List;

public class HybridScheduler implements SchedulerStrategy {
    private final BestFitScheduler bestFitScheduler = new BestFitScheduler();
    private final ModifiedPsoScheduler modifiedPsoScheduler = new ModifiedPsoScheduler();
    private final AdaptiveDecisionEngine decisionEngine = new AdaptiveDecisionEngine();

    @Override
    public String name() {
        return "HYBRID";
    }

    @Override
    public int[] schedule(int taskCount, int vmCount, List<Double> vmCapacities) {
        WorkloadProfile profile = new WorkloadProfile(
            Math.min(1.0, taskCount / (double) (vmCount * 20)),
            Math.min(1.0, taskCount / 1000.0),
            Math.min(1.0, 0.1 + (taskCount % 10) / 20.0)
        );

        return switch (decisionEngine.decide(profile)) {
            case HEURISTIC -> bestFitScheduler.schedule(taskCount, vmCount, vmCapacities);
            case PSO, HYBRID -> modifiedPsoScheduler.schedule(taskCount, vmCount, vmCapacities);
        };
    }
}
