package com.cloudoptimizer.workload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class AlibabaTraceParser {
    public List<TraceVmRequest> parse(Path path) throws IOException {
        return Files.lines(path)
            .skip(1)
            .map(line -> line.split(","))
            .filter(parts -> parts.length >= 4)
            .map(parts -> new TraceVmRequest(
                Long.parseLong(parts[0].trim()),
                Integer.parseInt(parts[1].trim()),
                Integer.parseInt(parts[2].trim()),
                Integer.parseInt(parts[3].trim())
            ))
            .toList();
    }
}
