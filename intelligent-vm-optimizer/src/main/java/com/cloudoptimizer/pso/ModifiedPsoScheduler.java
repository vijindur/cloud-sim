package com.cloudoptimizer.pso;

import java.util.List;

public class ModifiedPsoScheduler extends PsoScheduler {

    public ModifiedPsoScheduler() {
        super(20, 15);
        this.velocityClamp = 0.25;
    }

    @Override
    public String name() {
        return "PSO_MODIFIED";
    }

    @Override
    public int[] schedule(int taskCount, int vmCount, List<Double> vmCapacities) {
        double threshold = 1e-3;
        double previousBest = Double.NEGATIVE_INFINITY;

        int[] bestSchedule = super.schedule(taskCount, vmCount, vmCapacities);
        for (int i = 0; i < maxIterations; i++) {
            int[] candidate = super.schedule(taskCount, vmCount, vmCapacities);
            double candidateFitness = evaluateFromSchedule(candidate, vmCapacities);
            if (Math.abs(candidateFitness - previousBest) < threshold) {
                return candidate;
            }
            previousBest = candidateFitness;
            bestSchedule = candidate;
        }
        return bestSchedule;
    }

    @Override
    protected void updateParticles(List<Particle> particles, double[] globalBest, int iteration) {
        double startInertia = 0.9;
        double endInertia = 0.4;
        this.inertiaWeight = startInertia - ((startInertia - endInertia) * (iteration / (double) maxIterations));
        this.cognitiveCoeff = 1.6;
        this.socialCoeff = 1.2;
        super.updateParticles(particles, globalBest, iteration);
    }

    private double evaluateFromSchedule(int[] schedule, List<Double> vmCapacities) {
        double[] position = new double[schedule.length];
        int vmCount = Math.max(1, vmCapacities.size());
        for (int i = 0; i < schedule.length; i++) {
            position[i] = schedule[i] / (double) vmCount;
        }
        return evaluate(position, vmCapacities);
    }
}
