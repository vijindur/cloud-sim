package com.cloudoptimizer.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ResultExporter {
    public void toCsv(List<SimulationResult> results, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        StringBuilder sb = new StringBuilder();
        sb.append("algorithm,workload,utilization,slaCompliance,energyEfficiency,energyConsumption,averageResponseTime\n");
        for (SimulationResult r : results) {
            sb.append(String.format("%s,%s,%.4f,%.4f,%.4f,%.4f,%.4f%n",
                r.algorithm(), r.workload(), r.utilization(), r.slaCompliance(),
                r.energyEfficiency(), r.energyConsumption(), r.averageResponseTime()));
        }
        Files.writeString(path, sb.toString());
    }

    public void toJson(List<SimulationResult> results, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(path.toFile(), results);
    }
}
