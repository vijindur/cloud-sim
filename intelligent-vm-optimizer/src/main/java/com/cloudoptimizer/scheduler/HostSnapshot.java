package com.cloudoptimizer.scheduler;

public record HostSnapshot(
    int hostIndex,
    int totalPes,
    double peMips,
    long totalRamMb,
    long totalBwMbps,
    long totalStorageMb,
    int rackId,
    int cpuGeneration,
    double idlePowerWatts,
    double maxPowerWatts
) {
    public double totalMips() {
        return totalPes * peMips;
    }
}
