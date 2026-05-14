package com.cloudoptimizer.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudoptimizer.scheduler.HostSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

class EnergyMonitorTest {
    private final EnergyMonitor monitor = new EnergyMonitor();

    @Test
    void usesHostSpecificPowerCurve() {
        HostSnapshot host = new HostSnapshot(0, 16, 1500, 65_536, 120_000, 2_000_000, 0, 2, 135, 315);

        assertEquals(135.0, monitor.estimateHostPower(host, 0.0), 0.001);
        assertEquals(315.0, monitor.estimateHostPower(host, 1.0), 0.001);
        assertTrue(monitor.estimateHostPower(host, 0.5) > monitor.estimateHostPower(host, 0.1));
    }

    @Test
    void calculatesTotalEnergyInKwh() {
        HostSnapshot host = new HostSnapshot(0, 16, 1500, 65_536, 120_000, 2_000_000, 0, 2, 100, 300);

        double kwh = monitor.totalEnergyKwh(List.of(host), List.of(0.5), 2.0);

        assertEquals(0.4, kwh, 0.001);
    }
}
