package com.cloudoptimizer.energy;

import java.util.List;

public class EnergyMonitor {
    public double estimateHostPower(double utilization) {
        double idlePower = 120.0;
        double maxPower = 250.0;
        return idlePower + (maxPower - idlePower) * Math.max(0, Math.min(1, utilization));
    }

    public double totalEnergyKwh(List<Double> hostUtilizations, double hours) {
        return hostUtilizations.stream()
            .mapToDouble(this::estimateHostPower)
            .sum() * hours / 1000.0;
    }
}
