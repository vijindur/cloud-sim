package com.cloudoptimizer.energy;

import com.cloudoptimizer.scheduler.HostSnapshot;
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

    public double estimateHostPower(HostSnapshot host, double utilization) {
        double boundedUtilization = Math.max(0, Math.min(1, utilization));
        return host.idlePowerWatts() + (host.maxPowerWatts() - host.idlePowerWatts()) * boundedUtilization;
    }

    public double totalEnergyKwh(List<HostSnapshot> hosts, List<Double> hostUtilizations, double hours) {
        double watts = 0.0;
        for (int i = 0; i < hostUtilizations.size(); i++) {
            HostSnapshot host = hosts.get(i);
            watts += estimateHostPower(host, hostUtilizations.get(i));
        }
        return watts * hours / 1000.0;
    }
}
