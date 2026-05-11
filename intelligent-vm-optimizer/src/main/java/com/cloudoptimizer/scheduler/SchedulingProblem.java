package com.cloudoptimizer.scheduler;

import java.util.List;
import java.util.Map;

public record SchedulingProblem(
    List<SchedulingRequest> requests,
    List<HostSnapshot> hosts,
    Map<String, Integer> previousAssignments
) {}
