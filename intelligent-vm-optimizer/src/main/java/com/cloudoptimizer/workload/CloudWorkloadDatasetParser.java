package com.cloudoptimizer.workload;

import com.cloudoptimizer.core.WorkloadType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class CloudWorkloadDatasetParser {
    private static final DateTimeFormatter DATE_TIME_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

    public List<WorkloadRequest> parse(Path path) throws IOException {
        try (var lines = Files.lines(path)) {
            return lines
                .skip(1)
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(this::parseLine)
                .toList();
        }
    }

    public List<WorkloadRequest> selectRequests(
        Path path,
        WorkloadType workloadType,
        int limit,
        int seed
    ) throws IOException {
        List<WorkloadRequest> all = parse(path);
        if (all.isEmpty()) {
            return List.of();
        }

        List<WorkloadRequest> sorted = new ArrayList<>(all);
        sorted.sort(Comparator.comparingLong(WorkloadRequest::submitTimeSeconds));
        List<WorkloadRequest> selected = switch (workloadType) {
            case STEADY -> pickSteady(sorted, limit, seed);
            case VARIABLE -> pickVariable(sorted, limit, seed);
            case BURST -> pickBurst(sorted, limit, seed);
        };
        selected.sort(Comparator.comparingLong(WorkloadRequest::submitTimeSeconds));
        return selected;
    }

    private WorkloadRequest parseLine(String line) {
        String[] parts = line.split(",");
        String jobId = parts[0].trim();
        long submitTime = toSeconds(parts[1].trim());
        int requestedCpu = parseInt(parts[4], 1);
        int usedCpu = parseInt(parts[5], requestedCpu);
        long requestedMemory = parseLong(parts[6], 1024);
        long usedMemory = parseLong(parts[7], requestedMemory);
        long duration = Math.max(1L, Math.round(parseDouble(parts[8], 60.0)));
        long queueWait = Math.max(0L, parseLong(parts[9], 0));
        String jobType = parts[11].trim();
        String priority = parts[12].trim();
        int nodeCount = parseInt(parts[13], 1);
        long interarrival = Math.max(0L, parseLong(parts[14], 0));

        return new WorkloadRequest(
            jobId,
            submitTime,
            Math.max(1, requestedCpu),
            Math.max(1, usedCpu),
            Math.max(512L, requestedMemory),
            Math.max(512L, usedMemory),
            duration,
            queueWait,
            jobType,
            priority,
            Math.max(1, nodeCount),
            interarrival
        );
    }

    private List<WorkloadRequest> pickSteady(List<WorkloadRequest> sorted, int limit, int seed) {
        List<WorkloadRequest> ranked = new ArrayList<>(sorted);
        ranked.sort(Comparator.comparingDouble(this::stabilityScore));
        return samplePreservingOrder(ranked, limit, seed);
    }

    private List<WorkloadRequest> pickVariable(List<WorkloadRequest> sorted, int limit, int seed) {
        return samplePreservingOrder(sorted, limit, seed);
    }

    private List<WorkloadRequest> pickBurst(List<WorkloadRequest> sorted, int limit, int seed) {
        List<WorkloadRequest> ranked = new ArrayList<>(sorted);
        ranked.sort(Comparator.comparingDouble(this::burstScore).reversed());
        return samplePreservingOrder(ranked, limit, seed);
    }

    private List<WorkloadRequest> samplePreservingOrder(List<WorkloadRequest> source, int limit, int seed) {
        if (limit <= 0 || source.size() <= limit) {
            return new ArrayList<>(source);
        }
        Random random = new Random(seed);
        List<WorkloadRequest> copy = new ArrayList<>(source);
        for (int i = copy.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            WorkloadRequest tmp = copy.get(i);
            copy.set(i, copy.get(j));
            copy.set(j, tmp);
        }
        List<WorkloadRequest> subset = new ArrayList<>(copy.subList(0, limit));
        subset.sort(Comparator.comparingLong(WorkloadRequest::submitTimeSeconds));
        return subset;
    }

    private double stabilityScore(WorkloadRequest request) {
        return request.queueWaitSeconds() * 0.35
            + request.interarrivalSeconds() * 0.20
            + request.cpuDemandScore() * 0.30
            + request.memoryDemandScore() / 1024.0 * 0.15;
    }

    private double burstScore(WorkloadRequest request) {
        return request.queueWaitSeconds() * 0.35
            + Math.max(1, 600 - request.interarrivalSeconds()) * 0.15
            + request.requestedCpuPes() * 18.0
            + request.requestedMemoryMb() / 1024.0 * 6.0
            + request.nodeCount() * 8.0;
    }

    private long toSeconds(String value) {
        LocalDateTime dateTime = LocalDateTime.parse(value, DATE_TIME_FORMAT);
        return dateTime.toEpochSecond(ZoneOffset.UTC);
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value.trim());
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim());
        } catch (RuntimeException ex) {
            return fallback;
        }
    }
}
