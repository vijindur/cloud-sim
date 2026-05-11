package com.cloudoptimizer.scheduler;

import java.util.Map;

public record SchedulingResult(
    int[] hostMapping,
    double fitnessScore,
    Map<String, Double> diagnostics
) {}
