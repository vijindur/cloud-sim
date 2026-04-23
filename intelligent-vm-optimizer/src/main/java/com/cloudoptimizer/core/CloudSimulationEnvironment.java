package com.cloudoptimizer.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
                        List<Host> hosts, List<Vm> vms, List<Cloudlet> cloudlets,
                        List<Integer> hostRackMap, List<Long> vmSizesMb) {}

    public State create(SimulationConfig config, int runSeed) {
        CloudSim simulation = new CloudSim();
        List<Integer> hostRackMap = new ArrayList<>();
        List<Host> hosts = createHosts(config, hostRackMap);
        Datacenter datacenter = new DatacenterSimple(simulation, hosts, new VmAllocationPolicySimple());
        DatacenterBrokerSimple broker = new DatacenterBrokerSimple(simulation);
        List<Long> vmSizesMb = new ArrayList<>();
        List<Vm> vms = createVms(config, runSeed, vmSizesMb);
        List<Cloudlet> cloudlets = createCloudlets(config.cloudletCount(), config.workloadType(), runSeed);
        broker.submitVmList(vms);
        broker.submitCloudletList(cloudlets);
        return new State(simulation, datacenter, broker, hosts, vms, cloudlets, hostRackMap, vmSizesMb);
    }

    private List<Host> createHosts(SimulationConfig config, List<Integer> hostRackMap) {
        List<Host> hosts = new ArrayList<>();
        for (SimulationConfig.HostTypeConfig type : config.hostTypes()) {
            for (int i = 0; i < type.count(); i++) {
                List<Pe> peList = new ArrayList<>();
                for (int p = 0; p < type.pes(); p++) {
                    peList.add(new PeSimple(type.peMips()));
                }
                Host host = new HostSimple(type.ramMb(), type.bwMbps(), type.storageMb(), peList);
                host.setVmScheduler(new VmSchedulerTimeShared());
                hosts.add(host);
                hostRackMap.add(type.rackId());
            }
        }
        return hosts;
    }

    private List<Vm> createVms(SimulationConfig config, int runSeed, List<Long> vmSizesMb) {
        List<Vm> vms = new ArrayList<>();
        List<SimulationConfig.VmTypeConfig> weightedTypes = new ArrayList<>();
        for (SimulationConfig.VmTypeConfig type : config.vmTypes()) {
            for (int i = 0; i < type.ratioWeight(); i++) {
                weightedTypes.add(type);
            }
        }
        Random random = new Random(runSeed);
        for (int i = 0; i < config.vmCount(); i++) {
            SimulationConfig.VmTypeConfig type = weightedTypes.get(random.nextInt(weightedTypes.size()));
            Vm vm = new VmSimple(type.mips(), type.pes());
            vm.setRam(type.ramMb()).setBw(type.bwMbps()).setSize(type.sizeMb());
            vm.setCloudletScheduler(new CloudletSchedulerTimeShared());
            vms.add(vm);
            vmSizesMb.add(type.sizeMb());
        }
        return vms;
    }

    private List<Cloudlet> createCloudlets(int count, WorkloadType workloadType, int runSeed) {
        List<Cloudlet> cloudlets = new ArrayList<>();
        UtilizationModelDynamic utilization = new UtilizationModelDynamic(0.5);
        Random random = new Random(runSeed * 31L);
        for (int i = 0; i < count; i++) {
            long length = switch (workloadType) {
                case STEADY -> 10_000;
                case VARIABLE -> 5_000 + random.nextInt(20) * 1_000L;
                case BURST -> (i % 5 == 0) ? 50_000 : 8_000;
            };
            Cloudlet cloudlet = new CloudletSimple(length, 1, utilization);
            cloudlet.setSizes(1024);
            cloudlets.add(cloudlet);
        }
        return cloudlets;
    }
}
