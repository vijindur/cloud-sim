package com.cloudoptimizer.core;

import com.cloudoptimizer.scheduler.HostSnapshot;
import com.cloudoptimizer.workload.WorkloadRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.cloudsimplus.allocationpolicies.VmAllocationPolicySimple;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudsimplus.schedulers.vm.VmSchedulerSpaceShared;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;

public class CloudSimulationEnvironment {
    public record State(
        CloudSimPlus simulation,
        Datacenter datacenter,
        DatacenterBrokerSimple broker,
        List<Host> hosts,
        List<Vm> serviceVms,
        List<HostSnapshot> hostSnapshots
    ) {}

    public State create(SimulationConfig config) {
        CloudSimPlus simulation = new CloudSimPlus();
        List<HostSnapshot> snapshots = new ArrayList<>();
        List<Host> hosts = createHosts(config, snapshots);
        Datacenter datacenter = new DatacenterSimple(simulation, hosts, new VmAllocationPolicySimple());
        DatacenterBrokerSimple broker = new DatacenterBrokerSimple(simulation);
        List<Vm> serviceVms = createServiceVms(snapshots);
        broker.submitVmList(serviceVms);
        return new State(simulation, datacenter, broker, hosts, serviceVms, snapshots);
    }

    public List<Cloudlet> createCloudlets(List<WorkloadRequest> requests, double baseMips) {
        if (requests.isEmpty()) {
            return List.of();
        }
        long firstSubmit = requests.stream().mapToLong(WorkloadRequest::submitTimeSeconds).min().orElse(0L);
        long lastSubmit = requests.stream().mapToLong(WorkloadRequest::submitTimeSeconds).max().orElse(firstSubmit);
        double horizonSeconds = Math.max(1.0, lastSubmit - firstSubmit);
        double submissionScale = Math.max(1.0, horizonSeconds / Math.max(60.0, requests.size() * 12.0));
        List<Cloudlet> cloudlets = new ArrayList<>();
        for (WorkloadRequest request : requests) {
            int pes = Math.max(1, Math.min(4, request.requestedCpuPes()));
            long length = Math.max(1L, Math.round(request.durationSeconds() * baseMips * request.requestedCpuPes()));
            double utilization = Math.max(
                0.15,
                Math.min(1.0, request.usedCpuPes() / (double) Math.max(1, request.requestedCpuPes()))
            );
            Cloudlet cloudlet = new CloudletSimple(length, pes, new UtilizationModelDynamic(utilization));
            cloudlet.setFileSize(Math.max(300L, request.requestedMemoryMb() / 4));
            cloudlet.setOutputSize(Math.max(300L, request.usedMemoryMb() / 8));
            cloudlet.setSizes(Math.max(1024L, request.requestedMemoryMb()));
            cloudlet.setSubmissionDelay(Math.max(0.0, (request.submitTimeSeconds() - firstSubmit) / submissionScale));
            cloudlets.add(cloudlet);
        }
        return cloudlets;
    }

    private List<Host> createHosts(SimulationConfig config, List<HostSnapshot> snapshots) {
        List<Host> hosts = new ArrayList<>();
        int hostIndex = 0;
        for (SimulationConfig.HostTypeConfig type : config.hostTypes()) {
            for (int i = 0; i < type.count(); i++) {
                List<Pe> peList = new ArrayList<>();
                for (int p = 0; p < type.pes(); p++) {
                    peList.add(new PeSimple(type.peMips()));
                }
                Host host = new HostSimple(type.ramMb(), type.bwMbps(), type.storageMb(), peList);
                host.setVmScheduler(new VmSchedulerSpaceShared());
                hosts.add(host);
                snapshots.add(new HostSnapshot(
                    hostIndex++,
                    type.pes(),
                    type.peMips(),
                    type.ramMb(),
                    type.bwMbps(),
                    type.storageMb(),
                    type.rackId(),
                    type.cpuGeneration()
                ));
            }
        }
        return hosts;
    }

    private List<Vm> createServiceVms(List<HostSnapshot> snapshots) {
        List<HostSnapshot> ordered = snapshots.stream()
            .sorted(Comparator.comparingInt(HostSnapshot::hostIndex))
            .toList();
        List<Vm> serviceVms = new ArrayList<>();
        for (HostSnapshot snapshot : ordered) {
            int vmPes = Math.max(1, Math.min(4, snapshot.totalPes() / 2));
            double vmMips = Math.max(500.0, snapshot.peMips() * 0.8);
            long vmRam = Math.max(2048L, Math.min(16_384L, snapshot.totalRamMb() / 4));
            long vmBw = Math.max(2_000L, Math.min(20_000L, snapshot.totalBwMbps() / 6));
            long vmSize = Math.max(10_000L, Math.min(80_000L, snapshot.totalStorageMb() / 20));
            Vm vm = new VmSimple(vmMips, vmPes);
            vm.setRam(vmRam)
                .setBw(vmBw)
                .setSize(vmSize);
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
            serviceVms.add(vm);
        }
        return serviceVms;
    }
}
