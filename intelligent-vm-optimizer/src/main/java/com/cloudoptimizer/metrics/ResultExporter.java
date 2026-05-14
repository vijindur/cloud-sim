package com.cloudoptimizer.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ResultExporter {
    public void toCsv(List<SimulationResult> results, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        StringBuilder sb = new StringBuilder();
        sb.append("algorithm,workload,runSeed,utilization,slaCompliance,energyEfficiency,energyConsumption,averageResponseTime,responseP95,migrationCost,hostOverloadRate,throughputJobsPerTime,makespan,migrationCount,migrationDowntimeMs,topologyPenalty,networkDelayMs,acceptedRequests,queuedRequests,rejectedRequests,acceptanceRate,avgQueueDelay,convergenceStart,convergenceEnd\n");
        for (SimulationResult r : results) {
            sb.append(String.format("%s,%s,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f%n",
                r.algorithm(), r.workload(), r.runSeed(), r.utilization(), r.slaCompliance(),
                r.energyEfficiency(), r.energyConsumption(), r.averageResponseTime(),
                r.responseP95(),
                r.migrationCost(),
                r.hostOverloadRate(),
                r.throughputJobsPerTime(),
                r.makespan(),
                r.extraMetrics().getOrDefault("migrationCount", 0.0),
                r.extraMetrics().getOrDefault("migrationDowntimeMs", 0.0),
                r.extraMetrics().getOrDefault("topologyPenalty", 0.0),
                r.extraMetrics().getOrDefault("networkDelayMs", 0.0),
                r.extraMetrics().getOrDefault("acceptedRequests", 0.0),
                r.extraMetrics().getOrDefault("queuedRequests", 0.0),
                r.extraMetrics().getOrDefault("rejectedRequests", 0.0),
                r.extraMetrics().getOrDefault("acceptanceRate", 0.0),
                r.extraMetrics().getOrDefault("avgQueueDelay", 0.0),
                r.extraMetrics().getOrDefault("convergenceStart", 0.0),
                r.extraMetrics().getOrDefault("convergenceEnd", 0.0)));
        }
        Files.writeString(path, sb.toString());
    }

    public void appendToCsv(List<SimulationResult> results, Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (SimulationResult r : results) {
            sb.append(String.format("%s,%s,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f%n",
                r.algorithm(), r.workload(), r.runSeed(), r.utilization(), r.slaCompliance(),
                r.energyEfficiency(), r.energyConsumption(), r.averageResponseTime(),
                r.responseP95(),
                r.migrationCost(),
                r.hostOverloadRate(),
                r.throughputJobsPerTime(),
                r.makespan(),
                r.extraMetrics().getOrDefault("migrationCount", 0.0),
                r.extraMetrics().getOrDefault("migrationDowntimeMs", 0.0),
                r.extraMetrics().getOrDefault("topologyPenalty", 0.0),
                r.extraMetrics().getOrDefault("networkDelayMs", 0.0),
                r.extraMetrics().getOrDefault("acceptedRequests", 0.0),
                r.extraMetrics().getOrDefault("queuedRequests", 0.0),
                r.extraMetrics().getOrDefault("rejectedRequests", 0.0),
                r.extraMetrics().getOrDefault("acceptanceRate", 0.0),
                r.extraMetrics().getOrDefault("avgQueueDelay", 0.0),
                r.extraMetrics().getOrDefault("convergenceStart", 0.0),
                r.extraMetrics().getOrDefault("convergenceEnd", 0.0)));
        }
        Files.writeString(path, sb.toString(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
    }

    public void toJson(List<SimulationResult> results, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(path.toFile(), results);
    }

    public void toSummaryCsv(List<SimulationResult> results, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Map<String, List<SimulationResult>> grouped = results.stream()
            .collect(Collectors.groupingBy(r -> r.algorithm() + "|" + r.workload()));

        StringBuilder sb = new StringBuilder();
        sb.append("algorithm,workload,runs,energyMean,energyStd,responseMean,responseStd,slaMean,slaStd,utilizationMean,utilizationStd,migrationMean,queueDelayMean,acceptanceRateMean,rejectedMean\n");
        for (Map.Entry<String, List<SimulationResult>> e : grouped.entrySet()) {
            String[] key = e.getKey().split("\\|");
            List<SimulationResult> group = e.getValue();
            sb.append(String.format("%s,%s,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f%n",
                key[0], key[1],
                group.size(),
                mean(group, Metric.ENERGY), std(group, Metric.ENERGY),
                mean(group, Metric.RESPONSE), std(group, Metric.RESPONSE),
                mean(group, Metric.SLA), std(group, Metric.SLA),
                mean(group, Metric.UTILIZATION), std(group, Metric.UTILIZATION),
                mean(group, Metric.MIGRATION),
                mean(group, Metric.QUEUE_DELAY),
                mean(group, Metric.ACCEPTANCE_RATE),
                mean(group, Metric.REJECTED)));
        }
        Files.writeString(path, sb.toString());
    }

    private enum Metric { ENERGY, RESPONSE, SLA, UTILIZATION, MIGRATION, QUEUE_DELAY, ACCEPTANCE_RATE, REJECTED }

    private double mean(List<SimulationResult> group, Metric metric) {
        DoubleSummaryStatistics stats = group.stream().mapToDouble(r -> value(r, metric)).summaryStatistics();
        return stats.getAverage();
    }

    private double std(List<SimulationResult> group, Metric metric) {
        double mean = mean(group, metric);
        double variance = group.stream()
            .mapToDouble(r -> Math.pow(value(r, metric) - mean, 2))
            .average().orElse(0.0);
        return Math.sqrt(variance);
    }

    private double value(SimulationResult r, Metric metric) {
        return switch (metric) {
            case ENERGY -> r.energyConsumption();
            case RESPONSE -> r.averageResponseTime();
            case SLA -> r.slaCompliance();
            case UTILIZATION -> r.utilization();
            case MIGRATION -> r.migrationCost();
            case QUEUE_DELAY -> r.extraMetrics().getOrDefault("avgQueueDelay", 0.0);
            case ACCEPTANCE_RATE -> r.extraMetrics().getOrDefault("acceptanceRate", 0.0);
            case REJECTED -> r.extraMetrics().getOrDefault("rejectedRequests", 0.0);
        };
    }
}
