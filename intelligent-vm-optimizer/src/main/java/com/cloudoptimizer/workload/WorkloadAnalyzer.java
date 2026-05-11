package com.cloudoptimizer.workload;

import java.util.List;

public class WorkloadAnalyzer {

    public record WorkloadProfile(double loadFactor, double arrivalRate, double variability) {}

    public WorkloadProfile analyze(List<Double> arrivals) {
        if (arrivals.isEmpty()) {
            return new WorkloadProfile(0.0, 0.0, 0.0);
        }

        double sum = arrivals.stream().mapToDouble(Double::doubleValue).sum();
        double mean = sum / arrivals.size();
        double variance = arrivals.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average().orElse(0.0);
        double stdev = Math.sqrt(variance);

        double loadFactor = Math.min(1.0, mean / 100.0);
        double arrivalRate = arrivals.size() / Math.max(1.0, arrivals.get(arrivals.size() - 1));
        double variability = mean == 0.0 ? 0.0 : Math.min(1.0, stdev / mean);

        return new WorkloadProfile(loadFactor, arrivalRate, variability);
    }

    public WorkloadProfile analyzeRequests(List<WorkloadRequest> requests, int hostCount, int totalPes) {
        if (requests.isEmpty()) {
            return new WorkloadProfile(0.0, 0.0, 0.0);
        }

        double totalDemand = requests.stream().mapToDouble(WorkloadRequest::cpuDemandScore).sum();
        double maxDemand = Math.max(1.0, hostCount * totalPes);
        double loadFactor = Math.min(1.0, totalDemand / maxDemand);

        long start = requests.stream().mapToLong(WorkloadRequest::submitTimeSeconds).min().orElse(0L);
        long end = requests.stream().mapToLong(WorkloadRequest::submitTimeSeconds).max().orElse(start);
        double horizon = Math.max(1.0, end - start + 1);
        double arrivalRate = requests.size() / horizon;

        double meanWait = requests.stream().mapToDouble(WorkloadRequest::queueWaitSeconds).average().orElse(0.0);
        double variance = requests.stream()
            .mapToDouble(r -> Math.pow(r.queueWaitSeconds() - meanWait, 2))
            .average().orElse(0.0);
        double variability = meanWait == 0.0 ? Math.min(1.0, Math.sqrt(variance) / 60.0)
            : Math.min(1.0, Math.sqrt(variance) / meanWait);

        return new WorkloadProfile(loadFactor, arrivalRate, variability);
    }
}
