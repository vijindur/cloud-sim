package com.cloudoptimizer.pso;

import java.util.List;
import com.cloudoptimizer.core.SimulationConfig;
import com.cloudoptimizer.scheduler.SchedulingProblem;
import com.cloudoptimizer.scheduler.SchedulingResult;

public class ModifiedPsoScheduler extends PsoScheduler {

    public ModifiedPsoScheduler() {
        this(SimulationConfig.FitnessWeights.defaults());
    }

    public ModifiedPsoScheduler(SimulationConfig.FitnessWeights weights) {
        super(20, 15, weights);
    }

    @Override
    public String name() {
        return "PSO_MODIFIED";
    }

    @Override
    public SchedulingResult schedule(SchedulingProblem problem) {
        SchedulingResult initial = super.schedule(problem);
        int[] bestSchedule = initial.hostMapping();
        double bestFitness = initial.fitnessScore();

        for (int i = 0; i < maxIterations; i++) {
            SchedulingResult candidateResult = super.schedule(problem);
            int[] candidate = candidateResult.hostMapping();
            double candidateFitness = candidateResult.fitnessScore();
            if (candidateFitness > bestFitness) {
                bestFitness = candidateFitness;
                bestSchedule = candidate;
            }
        }
        return new SchedulingResult(bestSchedule, bestFitness, diagnostics(problem, bestSchedule));
    }
}
