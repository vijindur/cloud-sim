package com.cloudoptimizer.core;

import com.cloudoptimizer.energy.EnergyMonitor;
import com.cloudoptimizer.metrics.SimulationResult;
import com.cloudoptimizer.scheduler.SchedulerFactory;
import com.cloudoptimizer.scheduler.SchedulerStrategy;
import java.util.List;

public class SimulationOrchestrator {
    private final CloudSimulationEnvironment environment = new CloudSimulationEnvironment();
    private final SchedulerFactory schedulerFactory = new SchedulerFactory();
    private final EnergyMonitor energyMonitor = new EnergyMonitor();

    public SimulationResult run(SimulationConfig config) {
        CloudSimulationEnvironment.State state = environment.create(config);
        SchedulerStrategy scheduler = schedulerFactory.fromName(config.algorithm());
        List<Double> vmCaps = state.vms().stream().map(vm -> vm.getMips() * vm.getPesNumber()).toList();
        int[] mapping = scheduler.schedule(state.cloudlets().size(), state.vms().size(), vmCaps);
        state.simulation().start();

        double energy = energyMonitor.totalEnergyKwh(
            state.hosts().stream().map(h -> h.getCpuPercentUtilization()).toList(), 1.0);

        return MetricComputer.fromSchedule(
            scheduler.name(),
            config.workloadType(),
            state.cloudlets(),
            state.vms(),
            state.hosts(),
            mapping,
            energy
        );
    }
}
