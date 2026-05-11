package com.cloudoptimizer.scheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractResourceAwareScheduler implements SchedulerStrategy {
    protected SchedulingResult buildResult(SchedulingProblem problem, int[] mapping, double score) {
        Map<String, Double> diagnostics = evaluate(problem, mapping);
        return new SchedulingResult(mapping, score, diagnostics);
    }

    protected Map<String, Double> evaluate(SchedulingProblem problem, int[] mapping) {
        List<HostSnapshot> hosts = problem.hosts();
        List<SchedulingRequest> requests = problem.requests();
        double[] usedPes = new double[hosts.size()];
        double[] usedRam = new double[hosts.size()];
        int migrations = 0;
        int overloadedRequests = 0;

        for (int i = 0; i < requests.size(); i++) {
            int hostIndex = sanitizeHostIndex(mapping[i], hosts.size());
            SchedulingRequest request = requests.get(i);
            HostSnapshot host = hosts.get(hostIndex);
            usedPes[hostIndex] += request.cpuPes();
            usedRam[hostIndex] += request.memoryMb();
            Integer previous = problem.previousAssignments().get(request.requestId());
            if (previous != null && previous != hostIndex) {
                migrations++;
            }
            if (usedPes[hostIndex] > host.totalPes() || usedRam[hostIndex] > host.totalRamMb()) {
                overloadedRequests++;
            }
        }

        double totalCpuUtil = 0.0;
        double totalRamUtil = 0.0;
        double balanceVariance = 0.0;
        double[] compositeLoad = new double[Math.max(1, hosts.size())];
        int activeHosts = 0;
        for (int i = 0; i < hosts.size(); i++) {
            HostSnapshot host = hosts.get(i);
            double cpuUtil = usedPes[i] / Math.max(1.0, host.totalPes());
            double ramUtil = usedRam[i] / Math.max(1.0, host.totalRamMb());
            double combined = (cpuUtil + ramUtil) / 2.0;
            compositeLoad[i] = combined;
            totalCpuUtil += Math.min(cpuUtil, 1.5);
            totalRamUtil += Math.min(ramUtil, 1.5);
            if (usedPes[i] > 0 || usedRam[i] > 0) {
                activeHosts++;
            }
        }

        double meanLoad = 0.0;
        for (double load : compositeLoad) {
            meanLoad += load;
        }
        meanLoad /= compositeLoad.length;
        for (double load : compositeLoad) {
            balanceVariance += Math.pow(load - meanLoad, 2);
        }
        balanceVariance /= compositeLoad.length;

        Map<String, Double> metrics = new HashMap<>();
        metrics.put("avgCpuUtilization", totalCpuUtil / hosts.size());
        metrics.put("avgRamUtilization", totalRamUtil / hosts.size());
        metrics.put("activeHosts", (double) activeHosts);
        metrics.put("overloadedRequests", (double) overloadedRequests);
        metrics.put("migrations", (double) migrations);
        metrics.put("balanceVariance", balanceVariance);
        return metrics;
    }

    protected int sanitizeHostIndex(int hostIndex, int size) {
        if (size <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(size - 1, hostIndex));
    }

    protected int chooseLeastOverloadedHost(
        SchedulingRequest request,
        List<HostSnapshot> hosts,
        double[] usedPes,
        double[] usedRam
    ) {
        int best = 0;
        double bestPenalty = Double.POSITIVE_INFINITY;
        for (int i = 0; i < hosts.size(); i++) {
            HostSnapshot host = hosts.get(i);
            double cpuPenalty = Math.max(0.0, (usedPes[i] + request.cpuPes()) - host.totalPes());
            double ramPenalty = Math.max(0.0, (usedRam[i] + request.memoryMb()) - host.totalRamMb()) / 1024.0;
            double penalty = cpuPenalty * 2.0 + ramPenalty;
            if (penalty < bestPenalty) {
                bestPenalty = penalty;
                best = i;
            }
        }
        return best;
    }
}
