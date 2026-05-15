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
    NetworkConfig network,
    AdmissionConfig admission,
    FitnessWeights fitnessWeights,
    List<Integer> runSeeds
) {
    public record HostTypeConfig(String name, int count, int pes, double peMips, long ramMb, long bwMbps, long storageMb,
                                 int rackId, int cpuGeneration, double idlePowerWatts, double maxPowerWatts) {
        public double effectiveIdlePowerWatts() {
            return idlePowerWatts > 0 ? idlePowerWatts : 120.0;
        }

        public double effectiveMaxPowerWatts() {
            return maxPowerWatts > effectiveIdlePowerWatts() ? maxPowerWatts : 250.0;
        }
    }

    public record VmTypeConfig(String name, int ratioWeight, int pes, double mips, long ramMb, long bwMbps, long sizeMb) {}

    public record NetworkConfig(
        double sameRackLatencyMs,
        double crossRackLatencyMs,
        double sameRackBandwidthMbps,
        double crossRackBandwidthMbps
    ) {
        public static NetworkConfig defaults() {
            return new NetworkConfig(0.2, 1.5, 10_000.0, 2_000.0);
        }

        public double effectiveSameRackLatencyMs() {
            return sameRackLatencyMs > 0 ? sameRackLatencyMs : defaults().sameRackLatencyMs();
        }

        public double effectiveCrossRackLatencyMs() {
            return crossRackLatencyMs > 0 ? crossRackLatencyMs : defaults().crossRackLatencyMs();
        }

        public double effectiveSameRackBandwidthMbps() {
            return sameRackBandwidthMbps > 0 ? sameRackBandwidthMbps : defaults().sameRackBandwidthMbps();
        }

        public double effectiveCrossRackBandwidthMbps() {
            return crossRackBandwidthMbps > 0 ? crossRackBandwidthMbps : defaults().crossRackBandwidthMbps();
        }
    }

    public record AdmissionConfig(boolean enabled, long maxQueueDelaySeconds, boolean allowOvercommit) {
        public static AdmissionConfig defaults() {
            return new AdmissionConfig(true, 300, false);
        }
    }

    public record FitnessWeights(
        double utilization,
        double sla,
        double energy,
        double responseTime,
        double migration,
        double consolidation
    ) {
        public static FitnessWeights defaults() {
            return new FitnessWeights(0.24, 0.22, 0.20, 0.16, 0.10, 0.08);
        }
    }

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
                new HostTypeConfig("legacy", 24, 8, 1000, 32768, 80_000, 1_000_000, 0, 1, 115, 245),
                new HostTypeConfig("balanced", 32, 16, 1500, 65536, 120_000, 2_000_000, 1, 2, 135, 315),
                new HostTypeConfig("compute", 24, 24, 2200, 131072, 200_000, 3_000_000, 2, 3, 170, 430)
            ),
            List.of(
                new VmTypeConfig("small", 5, 1, 500, 2048, 2_000, 10_000),
                new VmTypeConfig("medium", 3, 2, 1000, 4096, 3_000, 15_000),
                new VmTypeConfig("large", 2, 4, 2000, 8192, 5_000, 25_000)
            ),
            NetworkConfig.defaults(),
            AdmissionConfig.defaults(),
            FitnessWeights.defaults(),
            List.of(11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
        );
    }

    public NetworkConfig effectiveNetwork() {
        return network != null ? network : NetworkConfig.defaults();
    }

    public AdmissionConfig effectiveAdmission() {
        return admission != null ? admission : AdmissionConfig.defaults();
    }

    public FitnessWeights effectiveFitnessWeights() {
        return fitnessWeights != null ? fitnessWeights : FitnessWeights.defaults();
    }

    public static SimulationConfig fromJson(Path path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(path.toFile(), SimulationConfig.class);
    }
}
