package com.cloudoptimizer.experiments;

import com.cloudoptimizer.core.SimulationConfig;
import com.cloudoptimizer.core.SimulationOrchestrator;
import com.cloudoptimizer.core.WorkloadType;
import com.cloudoptimizer.metrics.ResultExporter;
import com.cloudoptimizer.metrics.SimulationResult;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ExperimentRunner {
    private static final List<String> ALGORITHMS = List.of(
        "FIRST_FIT", "BEST_FIT_DECREASING", "ROUND_ROBIN", "FCFS", "BEST_FIT", "PSO_STANDARD", "PSO_MODIFIED", "HYBRID");

    public List<SimulationResult> runAll(SimulationConfig baseConfig) {
        SimulationOrchestrator orchestrator = new SimulationOrchestrator();
        List<SimulationResult> results = new ArrayList<>();
        for (String algorithm : ALGORITHMS) {
            for (WorkloadType workloadType : WorkloadType.values()) {
                for (int seed : baseConfig.runSeeds()) {
                    SimulationConfig config = new SimulationConfig(
                        baseConfig.hostCount(), baseConfig.vmCount(), baseConfig.cloudletCount(),
                        baseConfig.timeSteps(), seed, workloadType, algorithm, baseConfig.repetitions(),
                        baseConfig.migrationCostPerGb(), baseConfig.migrationDowntimeMsPerGb(),
                        baseConfig.hostTypes(), baseConfig.vmTypes(), baseConfig.runSeeds());
                    results.add(orchestrator.run(config, seed));
                }
            }
        }
        return results;
    }

    public void export(List<SimulationResult> results) throws IOException {
        ResultExporter exporter = new ResultExporter();
        exporter.toCsv(results, Path.of("results/experiment_results.csv"));
        exporter.toJson(results, Path.of("results/experiment_results.json"));
        exporter.toSummaryCsv(results, Path.of("results/experiment_summary.csv"));
    }
}
