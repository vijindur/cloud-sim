package com.cloudoptimizer.pso;

import com.cloudoptimizer.scheduler.SchedulerStrategy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PsoScheduler implements SchedulerStrategy {
    protected final int populationSize;
    protected final int maxIterations;
    protected final Random random = new Random(42);
    protected final FitnessModel fitnessModel = new FitnessModel();

    public PsoScheduler(int populationSize, int maxIterations) {
        this.populationSize = populationSize;
        this.maxIterations = maxIterations;
    }

    @Override
    public String name() {
        return "PSO_STANDARD";
    }

    @Override
    public int[] schedule(int taskCount, int vmCount, List<Double> vmCapacities) {
        int dimensions = taskCount;
        List<Particle> particles = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            particles.add(new Particle(dimensions, random));
        }

        double[] globalBest = new double[dimensions];
        double globalBestFitness = Double.NEGATIVE_INFINITY;

        for (int iter = 0; iter < maxIterations; iter++) {
            for (Particle particle : particles) {
                double fitness = evaluate(particle.position);
                if (fitness > particle.bestFitness) {
                    particle.bestFitness = fitness;
                    particle.bestPosition = particle.position.clone();
                }
                if (fitness > globalBestFitness) {
                    globalBestFitness = fitness;
                    globalBest = particle.position.clone();
                }
            }
            updateParticles(particles, globalBest);
        }

        int[] mapping = new int[taskCount];
        for (int i = 0; i < taskCount; i++) {
            mapping[i] = (int) Math.floor(Math.abs(globalBest[i] * vmCount)) % vmCount;
        }
        return mapping;
    }

    protected void updateParticles(List<Particle> particles, double[] globalBest) {
        double inertia = 0.7;
        double c1 = 1.4;
        double c2 = 1.4;
        for (Particle p : particles) {
            for (int d = 0; d < p.position.length; d++) {
                double r1 = random.nextDouble();
                double r2 = random.nextDouble();
                p.velocity[d] = inertia * p.velocity[d]
                    + c1 * r1 * (p.bestPosition[d] - p.position[d])
                    + c2 * r2 * (globalBest[d] - p.position[d]);
                p.position[d] = Math.max(0, Math.min(1, p.position[d] + p.velocity[d]));
            }
        }
    }

    protected double evaluate(double[] position) {
        double utilization = 0.6 + (random.nextDouble() * 0.4);
        double sla = 0.7 + (random.nextDouble() * 0.3);
        double energy = 0.5 + (random.nextDouble() * 0.5);
        return fitnessModel.score(utilization, sla, energy);
    }
}
