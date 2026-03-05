package com.cloudoptimizer.scheduler;

import java.util.List;

public interface SchedulerStrategy {
    String name();
    int[] schedule(int taskCount, int vmCount, List<Double> vmCapacities);
}
