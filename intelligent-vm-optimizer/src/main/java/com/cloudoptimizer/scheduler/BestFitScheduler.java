package com.cloudoptimizer.scheduler;

import java.util.List;

public class BestFitScheduler extends AbstractResourceAwareScheduler {
    @Override
    public String name() {
        return "BEST_FIT";
    }

    @Override
    public SchedulingResult schedule(SchedulingProblem problem) {
        List<SchedulingRequest> requests = problem.requests();
        List<HostSnapshot> hosts = problem.hosts();
        int[] mapping = new int[requests.size()];
        double[] usedPes = new double[hosts.size()];
        double[] usedRam = new double[hosts.size()];

        for (int i = 0; i < requests.size(); i++) {
            SchedulingRequest request = requests.get(i);
            int bestHost = -1;
            double bestSlack = Double.POSITIVE_INFINITY;
            for (int hostIndex = 0; hostIndex < hosts.size(); hostIndex++) {
                HostSnapshot host = hosts.get(hostIndex);
                double cpuSlack = host.totalPes() - (usedPes[hostIndex] + request.cpuPes());
                double ramSlack = host.totalRamMb() - (usedRam[hostIndex] + request.memoryMb());
                if (cpuSlack >= 0 && ramSlack >= 0) {
                    double weightedSlack = cpuSlack * 0.65 + (ramSlack / 1024.0) * 0.35;
                    if (weightedSlack < bestSlack) {
                        bestSlack = weightedSlack;
                        bestHost = hostIndex;
                    }
                }
            }
            if (bestHost < 0) {
                bestHost = chooseLeastOverloadedHost(request, hosts, usedPes, usedRam);
            }
            mapping[i] = bestHost;
            usedPes[bestHost] += request.cpuPes();
            usedRam[bestHost] += request.memoryMb();
        }
        return buildResult(problem, mapping, 0.0);
    }
}
