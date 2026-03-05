package com.cloudoptimizer.scheduler;

import java.util.List;

public class RoundRobinScheduler implements SchedulerStrategy {
    @Override
    public String name() {
        return "ROUND_ROBIN";
    }

    @Override
    public int[] schedule(int taskCount, int vmCount, List<Double> vmCapacities) {
        int[] map = new int[taskCount];
        for (int i = 0; i < taskCount; i++) {
            map[i] = i % vmCount;
        }
        return map;
    }
}
