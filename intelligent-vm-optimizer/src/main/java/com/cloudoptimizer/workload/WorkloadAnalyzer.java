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
}
