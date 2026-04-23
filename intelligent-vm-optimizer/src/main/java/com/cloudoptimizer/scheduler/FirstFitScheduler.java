package com.cloudoptimizer.scheduler;

import java.util.List;

public class FirstFitScheduler implements SchedulerStrategy {
    @Override
    public String name() {
        return "FIRST_FIT";
    }

    @Override
    public int[] schedule(int taskCount, int vmCount, List<Double> vmCapacities) {
        int[] mapping = new int[taskCount];
        double[] used = new double[vmCount];
        for (int t = 0; t < taskCount; t++) {
            double demand = 1 + (t % 10);
            int target = 0;
            for (int v = 0; v < vmCount; v++) {
                if (used[v] + demand <= vmCapacities.get(v)) {
                    target = v;
                    used[v] += demand;
                    break;
                }
            }
            mapping[t] = target;
        }
        return mapping;
    }
}
