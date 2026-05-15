# Intelligent Resource Optimization for Virtual Machines in Cloud Environments

## System Architecture

The project is organized as a reproducible CloudSim Plus simulation pipeline:

- **core**: builds the CloudSim datacenter, creates hosts/VMs from configuration, orchestrates timeline simulation, and executes CloudSim cloudlets.
- **scheduler**: implements baseline and resource-aware placement strategies such as First Fit, Best Fit, Best Fit Decreasing, Round Robin, FCFS, and Hybrid.
- **pso**: provides standard and modified PSO schedulers with configurable multi-objective fitness weights.
- **decision**: selects heuristic or PSO routing for the Hybrid scheduler using workload characteristics.
- **energy**: estimates host-level and datacenter-level power using host-specific idle/max wattage.
- **workload**: parses the cloud workload dataset and trace-style inputs into scheduling requests.
- **metrics**: exports run-level CSV/JSON outputs and summary statistics.
- **experiments**: sweeps algorithms, workloads, and random seeds for repeatable comparisons.
- **analysis/python_analysis**: generates statistical reports and plots using a headless Matplotlib backend.
- **visualization/dashboard**: provides an interactive Streamlit dashboard over measured results and what-if projections.

## Realism Features

The final implementation models several production-inspired constraints:

- **Heterogeneous host pools** with different CPU, RAM, bandwidth, storage, rack, CPU generation, and power curves.
- **Config-driven VM population** using `vmCount` and weighted `vmTypes` instead of hardcoded service VMs.
- **Admission control** that can accept, queue, or reject work when capacity is exceeded.
- **Rack-aware network penalties** using configurable same-rack and cross-rack latency/bandwidth.
- **Migration cost and downtime** based on VM image size and available network bandwidth.
- **Scheduler-aware CloudSim binding** that maps accepted workloads to VMs near the scheduler-selected host.
- **Statistical experiment outputs** including mean/std summaries, confidence intervals, ANOVA report, and ablation tables.

## Hybrid Scheduler

The Hybrid scheduler combines Best Fit and Modified PSO:

- Low-load and stable workloads use a fast heuristic path.
- High-load or highly variable workloads use PSO optimization.
- Mid-range workloads use the PSO-backed hybrid fallback.

This gives the project a practical tradeoff: low scheduling overhead when the cluster is calm, and deeper search when placement quality matters.

## Fitness Function

PSO fitness is configurable in `config/simulation-config.json`:

```text
Fitness =
  w_utilization   * utilizationScore
+ w_sla           * slaScore
+ w_energy        * energyScore
+ w_responseTime  * responseScore
+ w_migration     * migrationScore
+ w_consolidation * consolidationScore
```

The weights are normalized at runtime, which makes experiments easier to tune and explain.

## Experimental Design

- Algorithms: First Fit, Best Fit Decreasing, Round Robin, FCFS, Best Fit, Standard PSO, Modified PSO, Hybrid.
- Workloads: steady, variable, burst.
- Repetitions/seeds: configured in `config/simulation-config.json`.
- Outputs: `results/experiment_results.csv`, `results/experiment_results.json`, and `results/experiment_summary.csv`.

For fast demos, use `config/smoke-config.json` with a single algorithm/workload filter before running the full sweep.
