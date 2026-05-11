package com.cloudoptimizer.metrics;

import java.util.Map;

public record SimulationResult(
    String algorithm,
    String workload,
    int runSeed,
    double utilization,
    double slaCompliance,
    double energyEfficiency,
    double energyConsumption,
    double averageResponseTime,
    double responseP95,
    double migrationCost,
    double hostOverloadRate,
    double throughputJobsPerTime,
    double makespan,
    Map<String, Double> extraMetrics
) {}
