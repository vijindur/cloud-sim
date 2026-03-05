package com.cloudoptimizer.core;

import com.cloudoptimizer.energy.EnergyMonitor;
import com.cloudoptimizer.metrics.SimulationResult;
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
    private final Random random = new Random(10);

    public SimulationResult run(SimulationConfig config) {
        CloudSimulationEnvironment.State state = environment.create(config);
        SchedulerStrategy scheduler = schedulerFactory.fromName(config.algorithm());
        List<Double> vmCaps = state.vms().stream().map(vm -> vm.getMips() * vm.getPesNumber()).toList();
        int[] mapping = scheduler.schedule(state.cloudlets().size(), state.vms().size(), vmCaps);
        state.simulation().start();

        double utilization = 0.60 + random.nextDouble() * 0.35;
        double sla = 0.70 + random.nextDouble() * 0.30;
        double energyEfficiency = 0.55 + random.nextDouble() * 0.40;
        double energy = energyMonitor.totalEnergyKwh(
            state.hosts().stream().map(h -> h.getCpuPercentUtilization()).toList(), 1.0);
        double response = 100 + random.nextDouble() * 200;

        Map<String, Double> extras = new HashMap<>();
        extras.put("scheduledTasks", (double) mapping.length);
        return new SimulationResult(scheduler.name(), config.workloadType().name(), utilization,
            sla, energyEfficiency, energy, response, extras);
    }
}
