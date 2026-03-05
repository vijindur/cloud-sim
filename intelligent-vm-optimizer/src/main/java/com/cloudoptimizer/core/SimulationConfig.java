package com.cloudoptimizer.core;

public record SimulationConfig(
    int hostCount,
    int vmCount,
    int cloudletCount,
    WorkloadType workloadType,
    String algorithm,
    int repetitions
) {
    public static SimulationConfig defaultConfig() {
        return new SimulationConfig(80, 600, 1200, WorkloadType.VARIABLE, "HYBRID", 1);
    }
}
