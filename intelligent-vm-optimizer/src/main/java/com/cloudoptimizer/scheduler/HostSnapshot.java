package com.cloudoptimizer.scheduler;

public record HostSnapshot(
    int hostIndex,
    int totalPes,
    double peMips,
    long totalRamMb,
    long totalBwMbps,
    long totalStorageMb,
    int rackId,
    int cpuGeneration
) {
    public double totalMips() {
        return totalPes * peMips;
    }
}
