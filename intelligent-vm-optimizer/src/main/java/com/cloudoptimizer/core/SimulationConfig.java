package com.cloudoptimizer.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public record SimulationConfig(
    int hostCount,
    int vmCount,
    int cloudletCount,
    int timeSteps,
    int runSeed,
    WorkloadType workloadType,
    String algorithm,
    int repetitions,
    double migrationCostPerGb,
    double migrationDowntimeMsPerGb,
    String workloadDatasetPath,
    double responseTimeSlaFactor,
    List<HostTypeConfig> hostTypes,
    List<VmTypeConfig> vmTypes,
    List<Integer> runSeeds
) {
    public record HostTypeConfig(String name, int count, int pes, double peMips, long ramMb, long bwMbps, long storageMb,
                                 int rackId, int cpuGeneration) {}

    public record VmTypeConfig(String name, int ratioWeight, int pes, double mips, long ramMb, long bwMbps, long sizeMb) {}

    public static SimulationConfig defaultConfig() {
        return new SimulationConfig(
            80,
            600,
            1200,
            10,
            10,
            WorkloadType.VARIABLE,
            "HYBRID",
            1,
            0.15,
            250,
            "datasets/cloud_workload_dataset.csv",
            1.25,
            List.of(
                new HostTypeConfig("legacy", 24, 8, 1000, 32768, 80_000, 1_000_000, 0, 1),
                new HostTypeConfig("balanced", 32, 16, 1500, 65536, 120_000, 2_000_000, 1, 2),
                new HostTypeConfig("compute", 24, 24, 2200, 131072, 200_000, 3_000_000, 2, 3)
            ),
            List.of(
                new VmTypeConfig("small", 5, 1, 500, 2048, 2_000, 10_000),
                new VmTypeConfig("medium", 3, 2, 1000, 4096, 3_000, 15_000),
                new VmTypeConfig("large", 2, 4, 2000, 8192, 5_000, 25_000)
            ),
            List.of(11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
        );
    }

    public static SimulationConfig fromJson(Path path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(path.toFile(), SimulationConfig.class);
    }
}
