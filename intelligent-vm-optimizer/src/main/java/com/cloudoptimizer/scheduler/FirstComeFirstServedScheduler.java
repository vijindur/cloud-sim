package com.cloudoptimizer.scheduler;

import java.util.List;

public class FirstComeFirstServedScheduler implements SchedulerStrategy {
    @Override
    public String name() {
        return "FCFS";
    }

    @Override
    public int[] schedule(int taskCount, int vmCount, List<Double> vmCapacities) {
        int[] map = new int[taskCount];
        int currentVm = 0;
        for (int i = 0; i < taskCount; i++) {
            map[i] = currentVm;
            if ((i + 1) % Math.max(1, taskCount / vmCount) == 0 && currentVm < vmCount - 1) {
                currentVm++;
            }
        }
        return map;
    }
}
