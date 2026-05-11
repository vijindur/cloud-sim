package com.cloudoptimizer.scheduler;

import com.cloudoptimizer.decision.AdaptiveDecisionEngine;
import com.cloudoptimizer.pso.ModifiedPsoScheduler;
import com.cloudoptimizer.scheduler.SchedulingProblem;
import com.cloudoptimizer.scheduler.SchedulingResult;
import com.cloudoptimizer.workload.WorkloadRequest;
import com.cloudoptimizer.workload.WorkloadAnalyzer.WorkloadProfile;
import com.cloudoptimizer.workload.WorkloadAnalyzer;
import java.util.List;

public class HybridScheduler implements SchedulerStrategy {
    private final BestFitScheduler bestFitScheduler = new BestFitScheduler();
    private final ModifiedPsoScheduler modifiedPsoScheduler = new ModifiedPsoScheduler();
    private final AdaptiveDecisionEngine decisionEngine = new AdaptiveDecisionEngine();
    private final WorkloadAnalyzer workloadAnalyzer = new WorkloadAnalyzer();

    @Override
    public String name() {
        return "HYBRID";
    }

    @Override
    public SchedulingResult schedule(SchedulingProblem problem) {
        List<WorkloadRequest> requests = problem.requests().stream()
            .map(r -> new WorkloadRequest(
                r.requestId(),
                r.submitTimeSeconds(),
                r.cpuPes(),
                r.cpuPes(),
                r.memoryMb(),
                r.memoryMb(),
                r.durationSeconds(),
                r.queueWaitSeconds(),
                "mixed",
                r.priorityLevel(),
                r.nodeCount(),
                0L
            ))
            .toList();
        int totalPes = problem.hosts().stream().mapToInt(HostSnapshot::totalPes).sum();
        WorkloadProfile profile = workloadAnalyzer.analyzeRequests(requests, problem.hosts().size(), totalPes);

        return switch (decisionEngine.decide(profile)) {
            case HEURISTIC -> bestFitScheduler.schedule(problem);
            case PSO, HYBRID -> modifiedPsoScheduler.schedule(problem);
        };
    }
}
