package com.cloudoptimizer;

import com.cloudoptimizer.experiments.ExperimentRunner;
import com.cloudoptimizer.metrics.SimulationResult;
import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        ExperimentRunner runner = new ExperimentRunner();
        List<SimulationResult> results = runner.runAll();
        runner.export(results);
        System.out.printf("Completed %d simulation runs.%n", results.size());
    }
}
