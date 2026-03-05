package com.cloudoptimizer.workload;

public record TraceVmRequest(long timestamp, int cpu, int memory, int duration) {}
