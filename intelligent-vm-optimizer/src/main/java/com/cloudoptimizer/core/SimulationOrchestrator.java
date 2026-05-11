package com.cloudoptimizer.core;

import com.cloudoptimizer.energy.EnergyMonitor;
import com.cloudoptimizer.metrics.SimulationResult;
import com.cloudoptimizer.pso.BaseMetaHeuristicScheduler;
import com.cloudoptimizer.scheduler.HostSnapshot;
import com.cloudoptimizer.scheduler.SchedulerFactory;
import com.cloudoptimizer.scheduler.SchedulerStrategy;
import com.cloudoptimizer.scheduler.SchedulingProblem;
import com.cloudoptimizer.scheduler.SchedulingRequest;
import com.cloudoptimizer.scheduler.SchedulingResult;
import com.cloudoptimizer.workload.CloudWorkloadDatasetParser;
import com.cloudoptimizer.workload.WorkloadRequest;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.vms.Vm;

public class SimulationOrchestrator {
    private final CloudSimulationEnvironment environment = new CloudSimulationEnvironment();
    private final SchedulerFactory schedulerFactory = new SchedulerFactory();
    private final EnergyMonitor energyMonitor = new EnergyMonitor();
    private final CloudWorkloadDatasetParser workloadParser = new CloudWorkloadDatasetParser();

    public SimulationResult run(SimulationConfig config, int runSeed) {
        List<WorkloadRequest> requests = loadRequests(config, runSeed);
        if (requests.isEmpty()) {
            return new SimulationResult(config.algorithm(), config.workloadType().name(), runSeed, 0.0, 0.0, 0.0, 0.0, 0.0, Map.of());
        }

        CloudSimulationEnvironment.State state = environment.create(config);
        SchedulerStrategy scheduler = schedulerFactory.fromName(config.algorithm());
        TimelineMetrics timeline = simulateTimeline(config, scheduler, requests, state.hostSnapshots());
        CloudSimMetrics cloudSimMetrics = executeInCloudSim(state, requests, timeline.cloudletAssignments(), config.responseTimeSlaFactor());

        double utilization = timeline.averageUtilization();
        double responseTime = cloudSimMetrics.averageResponseTime();
        double slaCompliance = Math.max(
            0.0,
            1.0 - ((timeline.slaViolationRate() * 0.65) + (cloudSimMetrics.slaViolationRate() * 0.35))
        );
        double energyConsumption = timeline.energyKwh();
        double completedWork = requests.stream()
            .mapToDouble(r -> r.requestedCpuPes() * (double) r.durationSeconds())
            .sum();
        double energyEfficiency = Math.max(0.0, Math.min(1.0, completedWork / Math.max(1.0, energyConsumption * 20_000.0)));

        Map<String, Double> extras = new LinkedHashMap<>();
        extras.put("migrationCount", (double) timeline.migrationCount());
        extras.put("migrationCost", timeline.migrationCost());
        extras.put("migrationDowntimeMs", timeline.migrationDowntimeMs());
        extras.put("topologyPenalty", timeline.topologyPenalty());
        extras.put("hostOverloadRate", timeline.hostOverloadRate());
        extras.put("avgQueueDelay", cloudSimMetrics.averageQueueDelay());
        extras.put("makespan", cloudSimMetrics.makespan());
        extras.put("throughputJobsPerTime", cloudSimMetrics.throughput());
        extras.put("responseP95", cloudSimMetrics.responseP95());
        extras.put("timelineSteps", (double) config.timeSteps());
        if (scheduler instanceof BaseMetaHeuristicScheduler meta && !meta.lastConvergenceCurve().isEmpty()) {
            extras.put("convergenceStart", meta.lastConvergenceCurve().get(0));
            extras.put("convergenceEnd", meta.lastConvergenceCurve().get(meta.lastConvergenceCurve().size() - 1));
        }

        return new SimulationResult(
            scheduler.name(),
            config.workloadType().name(),
            runSeed,
            utilization,
            slaCompliance,
            energyEfficiency,
            energyConsumption,
            responseTime,
            extras
        );
    }

    private List<WorkloadRequest> loadRequests(SimulationConfig config, int runSeed) {
        try {
            List<WorkloadRequest> requests = workloadParser.selectRequests(
                Path.of(config.workloadDatasetPath()),
                config.workloadType(),
                config.cloudletCount(),
                runSeed
            );
            return normalizeTimeline(requests);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load workload dataset from " + config.workloadDatasetPath(), ex);
        }
    }

    private List<WorkloadRequest> normalizeTimeline(List<WorkloadRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }
        long minSubmit = requests.stream().mapToLong(WorkloadRequest::submitTimeSeconds).min().orElse(0L);
        long maxSubmit = requests.stream().mapToLong(WorkloadRequest::submitTimeSeconds).max().orElse(minSubmit);
        double horizon = Math.max(1.0, maxSubmit - minSubmit);
        double targetHorizon = Math.max(120.0, requests.size() * 18.0);
        double scale = Math.max(1.0, horizon / targetHorizon);

        return requests.stream()
            .map(request -> new WorkloadRequest(
                request.jobId(),
                Math.max(0L, Math.round((request.submitTimeSeconds() - minSubmit) / scale)),
                request.requestedCpuPes(),
                request.usedCpuPes(),
                request.requestedMemoryMb(),
                request.usedMemoryMb(),
                request.durationSeconds(),
                request.queueWaitSeconds(),
                request.jobType(),
                request.priorityLevel(),
                request.nodeCount(),
                Math.max(0L, Math.round(request.interarrivalSeconds() / scale))
            ))
            .sorted(Comparator.comparingLong(WorkloadRequest::submitTimeSeconds))
            .toList();
    }

    private TimelineMetrics simulateTimeline(
        SimulationConfig config,
        SchedulerStrategy scheduler,
        List<WorkloadRequest> requests,
        List<HostSnapshot> hosts
    ) {
        List<WorkloadRequest> ordered = requests.stream()
            .sorted(Comparator.comparingLong(WorkloadRequest::submitTimeSeconds))
            .toList();
        long start = ordered.get(0).submitTimeSeconds();
        long end = ordered.stream().mapToLong(WorkloadRequest::estimatedFinishTimeSeconds).max().orElse(start + 1);
        long stepDuration = Math.max(1L, (end - start) / Math.max(1, config.timeSteps()));

        Map<String, Integer> previousAssignments = new HashMap<>();
        Map<String, Integer> firstAssignments = new HashMap<>();
        double totalEnergy = 0.0;
        double totalUtilization = 0.0;
        double totalOverloadRate = 0.0;
        double totalTopologyPenalty = 0.0;
        int migrationCount = 0;
        double migrationCost = 0.0;
        double migrationDowntime = 0.0;
        int slaViolations = 0;
        int totalScheduledRequests = 0;

        for (int step = 0; step < config.timeSteps(); step++) {
            long windowStart = start + (step * stepDuration);
            long windowEnd = (step == config.timeSteps() - 1) ? end + 1 : windowStart + stepDuration;
            List<WorkloadRequest> active = ordered.stream()
                .filter(r -> r.submitTimeSeconds() < windowEnd && r.estimatedFinishTimeSeconds() > windowStart)
                .toList();
            if (active.isEmpty()) {
                continue;
            }

            List<SchedulingRequest> schedulingRequests = active.stream()
                .map(SchedulingRequest::fromWorkload)
                .toList();
            SchedulingProblem problem = new SchedulingProblem(schedulingRequests, hosts, previousAssignments);
            SchedulingResult result = scheduler.schedule(problem);
            int[] mapping = result.hostMapping();

            double[] hostCpuUse = new double[hosts.size()];
            double[] hostRamUse = new double[hosts.size()];
            int overloadCount = 0;

            for (int i = 0; i < active.size(); i++) {
                WorkloadRequest request = active.get(i);
                int hostIndex = Math.max(0, Math.min(hosts.size() - 1, mapping[i]));
                HostSnapshot host = hosts.get(hostIndex);

                hostCpuUse[hostIndex] += request.requestedCpuPes();
                hostRamUse[hostIndex] += request.requestedMemoryMb();
                totalTopologyPenalty += topologyPenalty(host, request);
                totalScheduledRequests++;

                Integer previousHost = previousAssignments.get(request.jobId());
                if (previousHost != null && previousHost != hostIndex) {
                    migrationCount++;
                    migrationCost += request.vmImageSizeGb() * config.migrationCostPerGb();
                    migrationDowntime += request.vmImageSizeGb() * config.migrationDowntimeMsPerGb();
                }
                previousAssignments.put(request.jobId(), hostIndex);
                firstAssignments.putIfAbsent(request.jobId(), hostIndex);
            }

            List<Double> utilizations = new ArrayList<>();
            for (int hostIndex = 0; hostIndex < hosts.size(); hostIndex++) {
                HostSnapshot host = hosts.get(hostIndex);
                double cpuUtil = hostCpuUse[hostIndex] / Math.max(1.0, host.totalPes());
                double ramUtil = hostRamUse[hostIndex] / Math.max(1.0, host.totalRamMb());
                double combined = (cpuUtil * 0.65) + (ramUtil * 0.35);
                utilizations.add(Math.max(0.0, combined));
                if (cpuUtil > 1.0 || ramUtil > 1.0) {
                    overloadCount++;
                }
            }

            double stepUtilization = utilizations.stream().mapToDouble(v -> Math.min(1.0, v)).average().orElse(0.0);
            totalUtilization += stepUtilization;
            totalEnergy += energyMonitor.totalEnergyKwh(utilizations, stepDuration / 3600.0);
            totalOverloadRate += overloadCount / (double) Math.max(1, hosts.size());
            slaViolations += overloadCount;
        }

        double avgUtilization = totalUtilization / Math.max(1, config.timeSteps());
        double avgOverloadRate = totalOverloadRate / Math.max(1, config.timeSteps());
        double slaViolationRate = slaViolations / (double) Math.max(1, totalScheduledRequests);

        return new TimelineMetrics(
            avgUtilization,
            totalEnergy,
            migrationCount,
            migrationCost,
            migrationDowntime,
            totalTopologyPenalty,
            avgOverloadRate,
            slaViolationRate,
            firstAssignments
        );
    }

    private CloudSimMetrics executeInCloudSim(
        CloudSimulationEnvironment.State state,
        List<WorkloadRequest> requests,
        Map<String, Integer> assignments,
        double responseTimeSlaFactor
    ) {
        double baseMips = state.hostSnapshots().stream().mapToDouble(HostSnapshot::peMips).average().orElse(1000.0);
        List<Cloudlet> cloudlets = environment.createCloudlets(requests, baseMips);
        Map<String, Cloudlet> cloudletById = new HashMap<>();
        for (int i = 0; i < requests.size(); i++) {
            cloudletById.put(requests.get(i).jobId(), cloudlets.get(i));
        }

        state.broker().submitCloudletList(cloudlets);
        List<Vm> vms = state.serviceVms();
        for (WorkloadRequest request : requests) {
            Cloudlet cloudlet = cloudletById.get(request.jobId());
            int hostIndex = Math.max(0, Math.min(vms.size() - 1, assignments.getOrDefault(request.jobId(), 0)));
            state.broker().bindCloudletToVm(cloudlet, vms.get(hostIndex));
        }

        state.simulation().start();
        List<Cloudlet> finished = state.broker().getCloudletFinishedList();
        if (finished.isEmpty()) {
            return new CloudSimMetrics(0.0, 1.0, 0.0, 0.0, 0.0);
        }

        double responseSum = 0.0;
        double waitingSum = 0.0;
        double finishMax = 0.0;
        int violations = 0;
        List<Double> responseTimes = new ArrayList<>();
        Map<Cloudlet, WorkloadRequest> requestByCloudlet = new HashMap<>();
        for (int i = 0; i < requests.size(); i++) {
            requestByCloudlet.put(cloudlets.get(i), requests.get(i));
        }

        for (Cloudlet cloudlet : finished) {
            WorkloadRequest request = requestByCloudlet.get(cloudlet);
            double response = cloudlet.getFinishTime() - cloudlet.getSubmissionDelay();
            double waiting = Math.max(0.0, cloudlet.getStartWaitTime());
            responseSum += response;
            waitingSum += waiting;
            finishMax = Math.max(finishMax, cloudlet.getFinishTime());
            responseTimes.add(response);
            if (response > request.durationSeconds() * responseTimeSlaFactor + request.queueWaitSeconds()) {
                violations++;
            }
        }

        responseTimes.sort(Double::compareTo);
        double responseP95 = responseTimes.get((int) Math.min(responseTimes.size() - 1, Math.floor(responseTimes.size() * 0.95)));
        double makespan = finishMax - finished.stream().mapToDouble(Cloudlet::getSubmissionDelay).min().orElse(0.0);
        double throughput = finished.size() / Math.max(1.0, makespan);
        return new CloudSimMetrics(
            responseSum / finished.size(),
            violations / (double) finished.size(),
            waitingSum / finished.size(),
            makespan,
            throughput,
            responseP95
        );
    }

    private double topologyPenalty(HostSnapshot host, WorkloadRequest request) {
        double rackPenalty = host.rackId() * 0.35;
        double generationBonus = Math.max(0.0, 4 - host.cpuGeneration()) * 0.08;
        return request.nodeCount() * (rackPenalty + generationBonus);
    }

    private record TimelineMetrics(
        double averageUtilization,
        double energyKwh,
        int migrationCount,
        double migrationCost,
        double migrationDowntimeMs,
        double topologyPenalty,
        double hostOverloadRate,
        double slaViolationRate,
        Map<String, Integer> cloudletAssignments
    ) {}

    private record CloudSimMetrics(
        double averageResponseTime,
        double slaViolationRate,
        double averageQueueDelay,
        double makespan,
        double throughput,
        double responseP95
    ) {
        CloudSimMetrics(double averageResponseTime, double slaViolationRate, double averageQueueDelay, double makespan, double throughput) {
            this(averageResponseTime, slaViolationRate, averageQueueDelay, makespan, throughput, averageResponseTime);
        }
    }
}
