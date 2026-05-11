package com.cloudoptimizer.workload;

public record WorkloadRequest(
    String jobId,
    long submitTimeSeconds,
    int requestedCpuPes,
    int usedCpuPes,
    long requestedMemoryMb,
    long usedMemoryMb,
    long durationSeconds,
    long queueWaitSeconds,
    String jobType,
    String priorityLevel,
    int nodeCount,
    long interarrivalSeconds
) {
    public double cpuDemandScore() {
        return requestedCpuPes + Math.max(0, usedCpuPes) * 0.35;
    }

    public double memoryDemandScore() {
        return requestedMemoryMb + Math.max(0, usedMemoryMb) * 0.15;
    }

    public long estimatedFinishTimeSeconds() {
        return submitTimeSeconds + queueWaitSeconds + durationSeconds;
    }

    public double vmImageSizeGb() {
        return Math.max(1.0, requestedMemoryMb / 1024.0 * 0.35);
    }
}
