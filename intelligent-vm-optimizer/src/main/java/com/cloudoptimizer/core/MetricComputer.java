package com.cloudoptimizer.core;

import com.cloudoptimizer.metrics.SimulationResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

public final class MetricComputer {
    private MetricComputer() {
    }

    public static SimulationResult fromSchedule(
        String algorithm,
        WorkloadType workloadType,
        List<Cloudlet> cloudlets,
        List<Vm> vms,
        List<Host> hosts,
        int[] mapping,
        double energyConsumption
    ) {
        int safeVmCount = Math.max(1, vms.size());
        double[] vmWorkload = new double[safeVmCount];
        double totalLength = 0;

        for (int i = 0; i < cloudlets.size(); i++) {
            Cloudlet cloudlet = cloudlets.get(i);
            double length = cloudlet.getLength();
            totalLength += length;
            int vmIndex = mapping.length == 0 ? 0 : Math.floorMod(mapping[i % mapping.length], safeVmCount);
            vmWorkload[vmIndex] += length;
        }

        double totalCapacity = 0;
        double makespan = 0;
        double totalResponseTime = 0;
        int activeVms = 0;

        for (int i = 0; i < safeVmCount; i++) {
            Vm vm = vms.get(i);
            double vmCapacity = Math.max(1.0, vm.getMips() * vm.getPesNumber());
            double completionTime = vmWorkload[i] / vmCapacity;
            makespan = Math.max(makespan, completionTime);
            totalResponseTime += completionTime;
            totalCapacity += vmCapacity;
            if (vmWorkload[i] > 0) {
                activeVms++;
            }
        }

        double normalizedLoad = totalLength / Math.max(1.0, totalCapacity);
        double utilization = Math.min(1.0, normalizedLoad / Math.max(1.0, makespan));
        double imbalance = computeImbalance(vmWorkload, totalLength);
        double slaCompliance = Math.max(0.0, 1.0 - imbalance * 0.8);

        double baselineEnergy = hosts.size() * 0.08;
        double energyEfficiency = 1.0 / (1.0 + Math.max(0.0, energyConsumption - baselineEnergy));

        double averageResponseTime = safeVmCount == 0 ? 0.0 : (totalResponseTime / safeVmCount) * 1000.0;

        Map<String, Double> extras = new HashMap<>();
        extras.put("scheduledTasks", (double) mapping.length);
        extras.put("activeVms", (double) activeVms);
        extras.put("makespan", makespan);
        return new SimulationResult(
            algorithm,
            workloadType.name(),
            utilization,
            slaCompliance,
            energyEfficiency,
            energyConsumption,
            averageResponseTime,
            extras
        );
    }

    public static double computeFitness(double[] position, List<Double> vmCapacities) {
        int vmCount = Math.max(1, vmCapacities.size());
        double[] vmLoad = new double[vmCount];

        for (int task = 0; task < position.length; task++) {
            int vmIndex = (int) Math.floor(Math.abs(position[task] * vmCount)) % vmCount;
            vmLoad[vmIndex] += 1.0;
        }

        double totalCapacity = vmCapacities.stream().mapToDouble(v -> Math.max(1.0, v)).sum();
        double effectiveCapacity = 0;
        double maxUtilization = 0;
        double energyPenalty = 0;

        for (int i = 0; i < vmCount; i++) {
            double capacity = Math.max(1.0, vmCapacities.get(i));
            double utilization = vmLoad[i] / capacity;
            maxUtilization = Math.max(maxUtilization, utilization);
            effectiveCapacity += Math.min(vmLoad[i], capacity);
            if (vmLoad[i] > 0) {
                energyPenalty += 0.2 + utilization * 0.8;
            }
        }

        double utilizationScore = Math.min(1.0, effectiveCapacity / Math.max(1.0, position.length));
        double slaScore = Math.max(0.0, 1.0 - Math.max(0.0, maxUtilization - 1.0));
        double normalizedEnergy = energyPenalty / Math.max(1.0, vmCount);
        double energyEfficiency = 1.0 / (1.0 + normalizedEnergy + (totalCapacity / Math.max(1.0, position.length * 10.0)));

        return 0.40 * utilizationScore + 0.35 * slaScore + 0.25 * energyEfficiency;
    }

    private static double computeImbalance(double[] vmWorkload, double totalLength) {
        if (vmWorkload.length == 0 || totalLength <= 0) {
            return 0.0;
        }
        double ideal = totalLength / vmWorkload.length;
        double variance = 0;
        for (double load : vmWorkload) {
            double delta = load - ideal;
            variance += delta * delta;
        }
        double stdev = Math.sqrt(variance / vmWorkload.length);
        return stdev / Math.max(1.0, ideal);
    }
}
