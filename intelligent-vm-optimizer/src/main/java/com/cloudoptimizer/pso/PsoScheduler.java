package com.cloudoptimizer.pso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import com.cloudoptimizer.scheduler.HostSnapshot;
import com.cloudoptimizer.scheduler.SchedulingProblem;
import com.cloudoptimizer.scheduler.SchedulingRequest;
import com.cloudoptimizer.scheduler.SchedulingResult;

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
    public SchedulingResult schedule(SchedulingProblem problem) {
        int dimensions = problem.requests().size();
        int hostCount = problem.hosts().size();
        lastConvergence = new ArrayList<>();
        List<Particle> particles = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            particles.add(new Particle(dimensions, random));
        }

        double[] globalBest = new double[dimensions];
        double globalBestFitness = Double.NEGATIVE_INFINITY;

        for (int iter = 0; iter < maxIterations; iter++) {
            for (Particle particle : particles) {
                double fitness = evaluate(problem, particle.position, hostCount);
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

        int[] mapping = repairMapping(problem, toMapping(globalBest, dimensions, hostCount));
        return new SchedulingResult(mapping, globalBestFitness, new HashMap<>(diagnostics(problem, mapping)));
    }

    protected int[] toMapping(double[] position, int taskCount, int vmCount) {
        int[] mapping = new int[taskCount];
        for (int i = 0; i < taskCount; i++) {
            mapping[i] = (int) Math.floor(Math.abs(position[i] * vmCount)) % vmCount;
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

    protected double evaluate(SchedulingProblem problem, double[] position, int hostCount) {
        int[] mapping = toMapping(position, position.length, hostCount);
        mapping = repairMapping(problem, mapping);
        return evaluateMapping(problem, mapping);
    }

    protected int[] repairMapping(SchedulingProblem problem, int[] mapping) {
        List<SchedulingRequest> requests = problem.requests();
        List<HostSnapshot> hosts = problem.hosts();
        double[] usedPes = new double[hosts.size()];
        double[] usedRam = new double[hosts.size()];
        int[] repaired = mapping.clone();

        for (int i = 0; i < requests.size(); i++) {
            SchedulingRequest request = requests.get(i);
            int target = sanitizeHostIndex(repaired[i], hosts.size());
            HostSnapshot host = hosts.get(target);
            if (usedPes[target] + request.cpuPes() > host.totalPes()
                || usedRam[target] + request.memoryMb() > host.totalRamMb()) {
                target = findBestFeasibleHost(request, hosts, usedPes, usedRam);
            }
            repaired[i] = target;
            usedPes[target] += request.cpuPes();
            usedRam[target] += request.memoryMb();
        }
        return repaired;
    }

    protected int findBestFeasibleHost(
        SchedulingRequest request,
        List<HostSnapshot> hosts,
        double[] usedPes,
        double[] usedRam
    ) {
        int bestHost = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int hostIndex = 0; hostIndex < hosts.size(); hostIndex++) {
            HostSnapshot host = hosts.get(hostIndex);
            double cpuRemaining = host.totalPes() - (usedPes[hostIndex] + request.cpuPes());
            double ramRemaining = host.totalRamMb() - (usedRam[hostIndex] + request.memoryMb());
            if (cpuRemaining >= 0 && ramRemaining >= 0) {
                double balance = 1.0 - Math.abs(cpuRemaining / Math.max(1.0, host.totalPes())
                    - ramRemaining / Math.max(1.0, host.totalRamMb()));
                double score = balance - (cpuRemaining / Math.max(1.0, host.totalPes())) * 0.35;
                if (score > bestScore) {
                    bestScore = score;
                    bestHost = hostIndex;
                }
            }
        }
        if (bestHost >= 0) {
            return bestHost;
        }
        return chooseLeastOverloadedHost(request, hosts, usedPes, usedRam);
    }
}
