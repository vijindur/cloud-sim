package com.cloudoptimizer.pso;

import com.cloudoptimizer.scheduler.AbstractResourceAwareScheduler;
import com.cloudoptimizer.scheduler.HostSnapshot;
import com.cloudoptimizer.scheduler.SchedulingProblem;
import com.cloudoptimizer.scheduler.SchedulingRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseMetaHeuristicScheduler extends AbstractResourceAwareScheduler {
    protected final FitnessModel fitnessModel = new FitnessModel();
    protected List<Double> lastConvergence = new ArrayList<>();

    protected double evaluateMapping(SchedulingProblem problem, int[] mapping) {
        List<HostSnapshot> hosts = problem.hosts();
        List<SchedulingRequest> requests = problem.requests();
        double[] usedPes = new double[hosts.size()];
        double[] usedRam = new double[hosts.size()];
        int overloadCount = 0;
        int migrationCount = 0;
        int activeHosts = 0;

        for (int i = 0; i < requests.size(); i++) {
            int hostIndex = sanitizeHostIndex(mapping[i], hosts.size());
            SchedulingRequest request = requests.get(i);
            HostSnapshot host = hosts.get(hostIndex);
            usedPes[hostIndex] += request.cpuPes();
            usedRam[hostIndex] += request.memoryMb();
            Integer previous = problem.previousAssignments().get(request.requestId());
            if (previous != null && previous != hostIndex) {
                migrationCount++;
            }
            if (usedPes[hostIndex] > host.totalPes() || usedRam[hostIndex] > host.totalRamMb()) {
                overloadCount++;
            }
        }

        double totalLoad = 0.0;
        double totalResponsePenalty = 0.0;
        double totalEnergyPenalty = 0.0;
        double totalVariance = 0.0;
        double[] hostLoad = new double[Math.max(1, hosts.size())];
        for (int i = 0; i < hosts.size(); i++) {
            HostSnapshot host = hosts.get(i);
            double cpuUtil = usedPes[i] / Math.max(1.0, host.totalPes());
            double ramUtil = usedRam[i] / Math.max(1.0, host.totalRamMb());
            double load = (cpuUtil * 0.65) + (ramUtil * 0.35);
            hostLoad[i] = load;
            totalLoad += Math.min(1.5, load);
            totalResponsePenalty += Math.max(0.0, load - 0.82);
            totalEnergyPenalty += load > 0 ? 0.25 + Math.max(0.0, load) : 0.0;
            if (load > 0) {
                activeHosts++;
            }
        }

        double meanLoad = totalLoad / hostLoad.length;
        for (double load : hostLoad) {
            totalVariance += Math.pow(load - meanLoad, 2);
        }
        totalVariance /= hostLoad.length;

        double utilizationScore = Math.max(0.0, 1.0 - Math.abs(0.72 - Math.min(meanLoad, 1.0)));
        double slaScore = Math.max(0.0, 1.0 - ((overloadCount / (double) Math.max(1, requests.size())) + totalResponsePenalty / hostLoad.length));
        double energyScore = Math.max(0.0, 1.0 - (totalEnergyPenalty / Math.max(1.0, hostLoad.length * 1.6)));
        double responseScore = Math.max(0.0, 1.0 - totalResponsePenalty / Math.max(1.0, hostLoad.length));
        double migrationScore = Math.max(0.0, 1.0 - migrationCount / (double) Math.max(1, requests.size()));
        double consolidationScore = Math.max(0.0, 1.0 - activeHosts / (double) Math.max(1, hosts.size()) + 0.2);
        return fitnessModel.score(
            utilizationScore,
            slaScore,
            energyScore,
            responseScore,
            migrationScore,
            consolidationScore
        );
    }

    protected Map<String, Double> diagnostics(SchedulingProblem problem, int[] mapping) {
        Map<String, Double> metrics = new HashMap<>(evaluate(problem, mapping));
        if (!lastConvergence.isEmpty()) {
            metrics.put("convergenceStart", lastConvergence.get(0));
            metrics.put("convergenceEnd", lastConvergence.get(lastConvergence.size() - 1));
        }
        return metrics;
    }

    public List<Double> lastConvergenceCurve() {
        return lastConvergence;
    }
}
