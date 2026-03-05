package com.cloudoptimizer.core;

import java.util.ArrayList;
import java.util.List;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;

public class CloudSimulationEnvironment {
    public record State(CloudSim simulation, Datacenter datacenter, DatacenterBrokerSimple broker,
                        List<Host> hosts, List<Vm> vms, List<Cloudlet> cloudlets) {}

    public State create(SimulationConfig config) {
        CloudSim simulation = new CloudSim();
        List<Host> hosts = createHosts(config.hostCount());
        Datacenter datacenter = new DatacenterSimple(simulation, hosts, new VmAllocationPolicySimple());
        DatacenterBrokerSimple broker = new DatacenterBrokerSimple(simulation);
        List<Vm> vms = createVms(config.vmCount());
        List<Cloudlet> cloudlets = createCloudlets(config.cloudletCount(), config.workloadType());
        broker.submitVmList(vms);
        broker.submitCloudletList(cloudlets);
        return new State(simulation, datacenter, broker, hosts, vms, cloudlets);
    }

    private List<Host> createHosts(int hostCount) {
        List<Host> hosts = new ArrayList<>();
        for (int i = 0; i < hostCount; i++) {
            List<Pe> peList = new ArrayList<>();
            int peCount = 8 + (i % 8);
            for (int p = 0; p < peCount; p++) {
                peList.add(new PeSimple(1000 + (i % 5) * 500));
            }
            Host host = new HostSimple(16384 + (i % 4) * 8192, 100000, 1_000_000, peList);
            host.setVmScheduler(new VmSchedulerTimeShared());
            hosts.add(host);
        }
        return hosts;
    }

    private List<Vm> createVms(int vmCount) {
        List<Vm> vms = new ArrayList<>();
        for (int i = 0; i < vmCount; i++) {
            Vm vm = new VmSimple(500 + (i % 4) * 250, 1 + (i % 4));
            vm.setRam(1024 + (i % 4) * 512).setBw(2000).setSize(10000);
            vm.setCloudletScheduler(new CloudletSchedulerTimeShared());
            vms.add(vm);
        }
        return vms;
    }

    private List<Cloudlet> createCloudlets(int count, WorkloadType workloadType) {
        List<Cloudlet> cloudlets = new ArrayList<>();
        UtilizationModelDynamic utilization = new UtilizationModelDynamic(0.5);
        for (int i = 0; i < count; i++) {
            long length = switch (workloadType) {
                case STEADY -> 10_000;
                case VARIABLE -> 5_000 + (i % 10) * 2_000L;
                case BURST -> (i % 5 == 0) ? 50_000 : 8_000;
            };
            Cloudlet cloudlet = new CloudletSimple(length, 1, utilization);
            cloudlet.setSizes(1024);
            cloudlets.add(cloudlet);
        }
        return cloudlets;
    }
}
