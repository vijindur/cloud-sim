package com.cloudoptimizer;

import com.cloudoptimizer.core.SimulationConfig;
import com.cloudoptimizer.experiments.ExperimentRunner;
import com.cloudoptimizer.metrics.SimulationResult;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws IOException {
        configureLogging();
        SimulationConfig config = loadConfig();

        ExperimentRunner runner = new ExperimentRunner();
        List<SimulationResult> results = runner.runAll(config);
        runner.export(results);

        LOGGER.info("Completed " + results.size() + " simulation runs.");
    }

    private static SimulationConfig loadConfig() {
        Path path = Path.of("config/simulation-config.json");
        try {
            return SimulationConfig.fromJson(path);
        } catch (IOException e) {
            LOGGER.warning("Could not load config from " + path + ", using defaults: " + e.getMessage());
            return SimulationConfig.defaultConfig();
        }
    }

    private static void configureLogging() throws IOException {
        FileHandler handler = new FileHandler("results/simulation.log", true);
        handler.setFormatter(new SimpleFormatter());
        Logger root = Logger.getLogger("");
        root.addHandler(handler);
        root.setLevel(Level.INFO);
    }
}
