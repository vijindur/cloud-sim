package com.cloudoptimizer.scheduler;

import java.util.List;

public class RoundRobinScheduler extends AbstractResourceAwareScheduler {
    @Override
    public String name() {
        return "ROUND_ROBIN";
    }

    @Override
    public SchedulingResult schedule(SchedulingProblem problem) {
        List<SchedulingRequest> requests = problem.requests();
        List<HostSnapshot> hosts = problem.hosts();
        int[] mapping = new int[requests.size()];
        double[] usedPes = new double[hosts.size()];
        double[] usedRam = new double[hosts.size()];
        int nextHost = 0;

        for (int i = 0; i < requests.size(); i++) {
            SchedulingRequest request = requests.get(i);
            int chosen = -1;
            for (int offset = 0; offset < hosts.size(); offset++) {
                int candidate = (nextHost + offset) % hosts.size();
                HostSnapshot host = hosts.get(candidate);
                if (usedPes[candidate] + request.cpuPes() <= host.totalPes()
                    && usedRam[candidate] + request.memoryMb() <= host.totalRamMb()) {
                    chosen = candidate;
                    break;
                }
            }
            if (chosen < 0) {
                chosen = chooseLeastOverloadedHost(request, hosts, usedPes, usedRam);
            }
            mapping[i] = chosen;
            usedPes[chosen] += request.cpuPes();
            usedRam[chosen] += request.memoryMb();
            nextHost = (chosen + 1) % hosts.size();
        }
        return buildResult(problem, mapping, 0.0);
    }
}
