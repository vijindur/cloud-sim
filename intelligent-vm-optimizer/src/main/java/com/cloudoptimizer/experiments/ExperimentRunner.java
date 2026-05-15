package com.cloudoptimizer.experiments;

import com.cloudoptimizer.core.SimulationConfig;
import com.cloudoptimizer.core.SimulationOrchestrator;
import com.cloudoptimizer.core.WorkloadType;
import com.cloudoptimizer.metrics.ResultExporter;
import com.cloudoptimizer.metrics.SimulationResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class ExperimentRunner {
    private static final List<String> ALGORITHMS = List.of(
        "FIRST_FIT", "BEST_FIT_DECREASING", "ROUND_ROBIN", "FCFS", "BEST_FIT", "PSO_STANDARD", "PSO_MODIFIED", "HYBRID");

    public List<SimulationResult> runAll(SimulationConfig baseConfig) {
        SimulationOrchestrator orchestrator = new SimulationOrchestrator();
        List<SimulationResult> results = new ArrayList<>();
        Set<String> algorithmFilter = parseFilter("simulation.algorithms");
        Set<String> workloadFilter = parseFilter("simulation.workloads");
        int maxSeeds = Integer.getInteger("simulation.maxSeeds", baseConfig.runSeeds().size());

        List<String> selectedAlgorithms = ALGORITHMS.stream()
            .filter(algo -> algorithmFilter.isEmpty() || algorithmFilter.contains(algo.toUpperCase(Locale.ROOT)))
            .toList();
        List<WorkloadType> selectedWorkloads = List.of(WorkloadType.values()).stream()
            .filter(w -> workloadFilter.isEmpty() || workloadFilter.contains(w.name()))
            .toList();
        List<Integer> selectedSeeds = baseConfig.runSeeds().stream().limit(Math.max(1, maxSeeds)).toList();
        int runsPerScenario = Math.max(1, baseConfig.repetitions());

        for (String algorithm : selectedAlgorithms) {
            for (WorkloadType workloadType : selectedWorkloads) {
                for (int run = 0; run < runsPerScenario; run++) {
                    int seed = selectedSeeds.get(run % selectedSeeds.size());
                    SimulationConfig config = new SimulationConfig(
                        baseConfig.hostCount(), baseConfig.vmCount(), baseConfig.cloudletCount(),
                        baseConfig.timeSteps(), seed, workloadType, algorithm, baseConfig.repetitions(),
                        baseConfig.migrationCostPerGb(), baseConfig.migrationDowntimeMsPerGb(),
                        baseConfig.workloadDatasetPath(), baseConfig.responseTimeSlaFactor(),
                        baseConfig.hostTypes(), baseConfig.vmTypes(),
                        baseConfig.effectiveNetwork(), baseConfig.effectiveAdmission(),
                        baseConfig.effectiveFitnessWeights(), baseConfig.runSeeds());
                    results.add(orchestrator.run(config, seed));
                }
            }
        }
        return results;
    }

    public void export(List<SimulationResult> results) throws IOException {
        Path resultsDir = Path.of("results");
        Files.createDirectories(resultsDir);

        ResultExporter exporter = new ResultExporter();
        Path csvPath = resultsDir.resolve("experiment_results.csv");
        Path jsonPath = resultsDir.resolve("experiment_results.json");
        Path summaryPath = resultsDir.resolve("experiment_summary.csv");
        
        // Append to CSV if it exists, otherwise create new
        if (Files.exists(csvPath)) {
            exporter.appendToCsv(results, csvPath);
        } else {
            exporter.toCsv(results, csvPath);
        }
        
        // Always overwrite JSON (for detailed results)
        exporter.toJson(results, jsonPath);
        
        // Overwrite summary (it's aggregated)
        exporter.toSummaryCsv(results, summaryPath);
    }

    private Set<String> parseFilter(String propertyName) {
        String value = System.getProperty(propertyName, "").trim();
        if (value.isEmpty()) {
            return Set.of();
        }
        return List.of(value.split(","))
            .stream()
            .map(v -> v.trim().toUpperCase(Locale.ROOT))
            .filter(v -> !v.isEmpty())
            .collect(Collectors.toSet());
    }
}
