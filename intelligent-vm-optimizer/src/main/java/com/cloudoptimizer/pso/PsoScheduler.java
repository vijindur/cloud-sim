package com.cloudoptimizer.pso;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PsoScheduler extends BaseMetaHeuristicScheduler {
    protected final int populationSize;
    protected final int maxIterations;
    protected final Random random = new Random(42);

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
        lastConvergence = new ArrayList<>();
        List<Particle> particles = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            particles.add(new Particle(dimensions, random));
        }

        double[] globalBest = new double[dimensions];
        double globalBestFitness = Double.NEGATIVE_INFINITY;

        for (int iter = 0; iter < maxIterations; iter++) {
            for (Particle particle : particles) {
                double fitness = evaluate(particle.position, vmCount);
                if (fitness > particle.bestFitness) {
                    particle.bestFitness = fitness;
                    particle.bestPosition = particle.position.clone();
                }
                if (fitness > globalBestFitness) {
                    globalBestFitness = fitness;
                    globalBest = particle.position.clone();
                }
            }
            lastConvergence.add(globalBestFitness);
            updateParticles(particles, globalBest);
        }

        return toMapping(globalBest, taskCount, vmCount);
    }

    protected int[] toMapping(double[] position, int taskCount, int vmCount) {
        int[] mapping = new int[taskCount];
        for (int i = 0; i < taskCount; i++) {
            mapping[i] = (int) Math.floor(Math.abs(position[i] * vmCount)) % vmCount;
        }
        return mapping;
    }

    protected void updateParticles(List<Particle> particles, double[] globalBest, int iteration) {
        for (Particle p : particles) {
            for (int d = 0; d < p.position.length; d++) {
                double r1 = random.nextDouble();
                double r2 = random.nextDouble();
                p.velocity[d] = inertiaWeight * p.velocity[d]
                    + cognitiveCoeff * r1 * (p.bestPosition[d] - p.position[d])
                    + socialCoeff * r2 * (globalBest[d] - p.position[d]);
                p.velocity[d] = Math.max(-velocityClamp, Math.min(velocityClamp, p.velocity[d]));
                p.position[d] = Math.max(0, Math.min(1, p.position[d] + p.velocity[d]));
            }
        }
    }

    protected double evaluate(double[] position, int vmCount) {
        int[] mapping = toMapping(position, position.length, vmCount);
        return evaluateMapping(mapping, vmCount);
    }
}
