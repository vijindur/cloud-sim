package com.cloudoptimizer;

import com.cloudoptimizer.core.SimulationConfig;
import com.cloudoptimizer.experiments.ExperimentRunner;
import com.cloudoptimizer.metrics.SimulationResult;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
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
        Path path = Path.of(System.getProperty("simulation.config", "config/simulation-config.json"));
        try {
            return SimulationConfig.fromJson(path);
        } catch (IOException e) {
            LOGGER.warning("Could not load config from " + path + ", using defaults: " + e.getMessage());
            return SimulationConfig.defaultConfig();
        }
    }

    private static void configureLogging() throws IOException {
        Path resultsDir = Path.of("results");
        java.nio.file.Files.createDirectories(resultsDir);

        Logger root = Logger.getLogger("");
        root.setLevel(Level.INFO);

        // Prevent duplicate file handlers if main() is invoked multiple times in the same JVM.
        for (Handler h : root.getHandlers()) {
            if (h instanceof FileHandler fh) {
                if (String.valueOf(fh).contains("simulation.log")) {
                    root.removeHandler(h);
                    try {
                        h.close();
                    } catch (Exception ignored) {
                        // best-effort cleanup
                    }
                }
            }
        }

        FileHandler handler = new FileHandler(resultsDir.resolve("simulation.log").toString(), true);
        handler.setFormatter(new SimpleFormatter());
        root.addHandler(handler);

        java.lang.Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                handler.close();
            } catch (Exception ignored) {
                // best-effort cleanup
            }
        }));
    }
}
