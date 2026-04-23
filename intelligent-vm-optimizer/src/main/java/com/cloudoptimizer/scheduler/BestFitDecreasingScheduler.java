package com.cloudoptimizer.scheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BestFitDecreasingScheduler implements SchedulerStrategy {
    @Override
    public String name() {
        return "BEST_FIT_DECREASING";
    }

    @Override
    public int[] schedule(int taskCount, int vmCount, List<Double> vmCapacities) {
        List<Integer> taskIds = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            taskIds.add(i);
        }
        taskIds.sort(Comparator.comparingDouble((Integer t) -> -(1 + (t % 10))));

        int[] mapping = new int[taskCount];
        double[] remaining = new double[vmCount];
        for (int i = 0; i < vmCount; i++) {
            remaining[i] = vmCapacities.get(i);
        }

        for (int task : taskIds) {
            double demand = 1 + (task % 10);
            int bestVm = 0;
            double tightest = Double.POSITIVE_INFINITY;
            for (int vm = 0; vm < vmCount; vm++) {
                if (remaining[vm] >= demand) {
                    double slack = remaining[vm] - demand;
                    if (slack < tightest) {
                        tightest = slack;
                        bestVm = vm;
                    }
                }
            }
            remaining[bestVm] = Math.max(0, remaining[bestVm] - demand);
            mapping[task] = bestVm;
        }
        return mapping;
    }
}
