package com.cloudoptimizer.scheduler;

import com.cloudoptimizer.workload.WorkloadRequest;

public record SchedulingRequest(
    String requestId,
    long submitTimeSeconds,
    long durationSeconds,
    int cpuPes,
    long memoryMb,
    long queueWaitSeconds,
    int nodeCount,
    String priorityLevel,
    double vmImageSizeGb
) {
    public static SchedulingRequest fromWorkload(WorkloadRequest request) {
        return new SchedulingRequest(
            request.jobId(),
            request.submitTimeSeconds(),
            request.durationSeconds(),
            request.requestedCpuPes(),
            request.requestedMemoryMb(),
            request.queueWaitSeconds(),
            request.nodeCount(),
            request.priorityLevel(),
            request.vmImageSizeGb()
        );
    }
}
