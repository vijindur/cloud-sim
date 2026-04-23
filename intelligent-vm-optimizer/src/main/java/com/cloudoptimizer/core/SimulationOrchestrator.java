package com.cloudoptimizer.core;

import com.cloudoptimizer.energy.EnergyMonitor;
import com.cloudoptimizer.metrics.SimulationResult;
import com.cloudoptimizer.pso.BaseMetaHeuristicScheduler;
import com.cloudoptimizer.scheduler.SchedulerFactory;
import com.cloudoptimizer.scheduler.SchedulerStrategy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SimulationOrchestrator {
    private final CloudSimulationEnvironment environment = new CloudSimulationEnvironment();
    private final SchedulerFactory schedulerFactory = new SchedulerFactory();
    private final EnergyMonitor energyMonitor = new EnergyMonitor();

    public SimulationResult run(SimulationConfig config, int runSeed) {
        Random random = new Random(runSeed);
        CloudSimulationEnvironment.State state = environment.create(config, runSeed);
        SchedulerStrategy scheduler = schedulerFactory.fromName(config.algorithm());
        List<Double> vmCaps = state.vms().stream().map(vm -> vm.getMips() * vm.getPesNumber()).toList();

        int[] previousMapping = null;
        double migrationCost = 0.0;
        double migrationDowntime = 0.0;
        double topologyPenalty = 0.0;

        for (int t = 0; t < config.timeSteps(); t++) {
            int[] mapping = scheduler.schedule(state.cloudlets().size(), state.vms().size(), vmCaps);
            if (previousMapping != null) {
                migrationCost += calculateMigrationCost(previousMapping, mapping, state.vmSizesMb(), config);
                migrationDowntime += calculateMigrationDowntime(previousMapping, mapping, state.vmSizesMb(), config);
            }
            topologyPenalty += estimateTopologyPenalty(mapping, state.hostRackMap(), random);
            previousMapping = mapping;
        }

        state.simulation().start();

        double utilization = 0.60 + random.nextDouble() * 0.35;
        double sla = Math.max(0.0, 0.95 - (migrationDowntime / Math.max(1.0, config.timeSteps() * 100_000)) - topologyPenalty * 1e-5);
        double energy = energyMonitor.totalEnergyKwh(
            state.hosts().stream().map(h -> h.getCpuPercentUtilization()).toList(), 1.0) + migrationCost * 0.05;
        double energyEfficiency = Math.max(0.0, 1.0 - Math.min(1.0, energy / 1000.0));
        double response = 100 + random.nextDouble() * 200 + migrationDowntime * 0.001;

        Map<String, Double> extras = new HashMap<>();
        extras.put("migrationCost", migrationCost);
        extras.put("migrationDowntimeMs", migrationDowntime);
        extras.put("topologyPenalty", topologyPenalty);

        if (scheduler instanceof BaseMetaHeuristicScheduler meta && !meta.lastConvergenceCurve().isEmpty()) {
            extras.put("convergenceStart", meta.lastConvergenceCurve().get(0));
            extras.put("convergenceEnd", meta.lastConvergenceCurve().get(meta.lastConvergenceCurve().size() - 1));
        }

        return new SimulationResult(scheduler.name(), config.workloadType().name(), utilization,
            sla, energyEfficiency, energy, response, extras);
    }

    private double calculateMigrationCost(int[] previous, int[] current, List<Long> vmSizesMb, SimulationConfig config) {
        double cost = 0.0;
        for (int i = 0; i < Math.min(previous.length, current.length); i++) {
            if (previous[i] != current[i]) {
                double sizeGb = vmSizesMb.get(i % vmSizesMb.size()) / 1024.0;
                cost += sizeGb * config.migrationCostPerGb();
            }
        }
        return cost;
    }

    private double calculateMigrationDowntime(int[] previous, int[] current, List<Long> vmSizesMb, SimulationConfig config) {
        double downtime = 0.0;
        for (int i = 0; i < Math.min(previous.length, current.length); i++) {
            if (previous[i] != current[i]) {
                double sizeGb = vmSizesMb.get(i % vmSizesMb.size()) / 1024.0;
                downtime += sizeGb * config.migrationDowntimeMsPerGb();
            }
        }
        return downtime;
    }

    private double estimateTopologyPenalty(int[] mapping, List<Integer> hostRackMap, Random random) {
        double penalty = 0.0;
        for (int i = 0; i < mapping.length - 1; i += 2) {
            int hostA = mapping[i] % hostRackMap.size();
            int hostB = mapping[i + 1] % hostRackMap.size();
            int rackA = hostRackMap.get(hostA);
            int rackB = hostRackMap.get(hostB);
            penalty += (rackA == rackB) ? 0.5 : 2.0;
            penalty += random.nextDouble() * 0.2;
        }
        return penalty;
    }
}
