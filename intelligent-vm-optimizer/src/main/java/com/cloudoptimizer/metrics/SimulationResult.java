package com.cloudoptimizer.metrics;

import java.util.Map;

public record SimulationResult(
    String algorithm,
    String workload,
    double utilization,
    double slaCompliance,
    double energyEfficiency,
    double energyConsumption,
    double averageResponseTime,
    Map<String, Double> extraMetrics
) {}
