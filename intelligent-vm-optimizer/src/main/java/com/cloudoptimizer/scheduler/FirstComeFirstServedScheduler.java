package com.cloudoptimizer.scheduler;

import java.util.List;

public class FirstComeFirstServedScheduler extends AbstractResourceAwareScheduler {
    @Override
    public String name() {
        return "FCFS";
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
            int chosen = -1;
            for (int hostIndex = 0; hostIndex < hosts.size(); hostIndex++) {
                HostSnapshot host = hosts.get(hostIndex);
                if (usedPes[hostIndex] + request.cpuPes() <= host.totalPes()
                    && usedRam[hostIndex] + request.memoryMb() <= host.totalRamMb()) {
                    chosen = hostIndex;
                    break;
                }
            }
            if (chosen < 0) {
                chosen = chooseLeastOverloadedHost(request, hosts, usedPes, usedRam);
            }
            mapping[i] = chosen;
            usedPes[chosen] += request.cpuPes();
            usedRam[chosen] += request.memoryMb();
        }
        return buildResult(problem, mapping, 0.0);
    }
}
