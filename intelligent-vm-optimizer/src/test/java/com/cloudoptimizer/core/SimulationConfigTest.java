package com.cloudoptimizer.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SimulationConfigTest {
    @Test
    void loadsRealisticSimulationConfig() throws Exception {
        SimulationConfig config = SimulationConfig.fromJson(Path.of("config/smoke-config.json"));

        assertEquals("HYBRID", config.algorithm());
        assertTrue(config.vmCount() > 0);
        assertTrue(config.hostTypes().stream().allMatch(host -> host.effectiveMaxPowerWatts() > host.effectiveIdlePowerWatts()));
        assertTrue(config.effectiveNetwork().effectiveCrossRackLatencyMs() > config.effectiveNetwork().effectiveSameRackLatencyMs());
        assertNotNull(config.effectiveAdmission());
        assertNotNull(config.effectiveFitnessWeights());
    }
}
