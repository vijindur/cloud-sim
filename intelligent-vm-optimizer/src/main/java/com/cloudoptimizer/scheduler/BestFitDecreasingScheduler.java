package com.cloudoptimizer.scheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BestFitDecreasingScheduler extends AbstractResourceAwareScheduler {
    @Override
    public String name() {
        return "BEST_FIT_DECREASING";
    }

    @Override
    public SchedulingResult schedule(SchedulingProblem problem) {
        List<SchedulingRequest> requests = problem.requests();
        List<HostSnapshot> hosts = problem.hosts();
        List<Integer> requestIds = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            requestIds.add(i);
        }
        requestIds.sort(Comparator.comparingDouble((Integer idx) ->
            -(requests.get(idx).cpuPes() + requests.get(idx).memoryMb() / 1024.0)));

        int[] mapping = new int[requests.size()];
        double[] usedPes = new double[hosts.size()];
        double[] usedRam = new double[hosts.size()];

        for (int requestIndex : requestIds) {
            SchedulingRequest request = requests.get(requestIndex);
            int bestHost = -1;
            double bestSlack = Double.POSITIVE_INFINITY;
            for (int hostIndex = 0; hostIndex < hosts.size(); hostIndex++) {
                HostSnapshot host = hosts.get(hostIndex);
                double cpuSlack = host.totalPes() - (usedPes[hostIndex] + request.cpuPes());
                double ramSlack = host.totalRamMb() - (usedRam[hostIndex] + request.memoryMb());
                if (cpuSlack >= 0 && ramSlack >= 0) {
                    double slack = cpuSlack * 0.65 + (ramSlack / 1024.0) * 0.35;
                    if (slack < bestSlack) {
                        bestSlack = slack;
                        bestHost = hostIndex;
                    }
                }
            }
            if (bestHost < 0) {
                bestHost = chooseLeastOverloadedHost(request, hosts, usedPes, usedRam);
            }
            mapping[requestIndex] = bestHost;
            usedPes[bestHost] += request.cpuPes();
            usedRam[bestHost] += request.memoryMb();
        }
        return buildResult(problem, mapping, 0.0);
    }
}
