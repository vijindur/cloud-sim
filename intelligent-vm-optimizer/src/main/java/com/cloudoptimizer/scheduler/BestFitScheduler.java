package com.cloudoptimizer.scheduler;

import java.util.Comparator;
import java.util.List;

public class BestFitScheduler implements SchedulerStrategy {
    @Override
    public String name() {
        return "BEST_FIT";
    }

    @Override
    public int[] schedule(int taskCount, int vmCount, List<Double> vmCapacities) {
        int[] map = new int[taskCount];
        for (int i = 0; i < taskCount; i++) {
            final double demand = 1 + (i % 10);
            int target = vmCapacities.stream()
                .sorted(Comparator.comparingDouble(c -> Math.abs(c - demand)))
                .map(vmCapacities::indexOf)
                .findFirst().orElse(0);
            map[i] = target;
        }
        return map;
    }
}
