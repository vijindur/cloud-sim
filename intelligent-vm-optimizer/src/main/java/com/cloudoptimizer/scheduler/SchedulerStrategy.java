package com.cloudoptimizer.scheduler;

public interface SchedulerStrategy {
    String name();
    SchedulingResult schedule(SchedulingProblem problem);
}
