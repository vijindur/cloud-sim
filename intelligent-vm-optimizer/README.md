# Intelligent VM Optimizer

Research-grade simulation framework for intelligent VM scheduling using CloudSim Plus.

## What is modeled now
- **Config-driven VM pool**: `vmCount` and weighted `vmTypes` now create the CloudSim VM pool instead of hardcoded service VMs.
- **Scheduler-aware CloudSim binding**: accepted workloads are bound to VMs closest to the scheduler-selected host pool.
- **Admission control**: overloaded placements can be queued or rejected, with acceptance/rejection metrics exported.
- **Host-specific power models**: each host type has idle/max wattage for more realistic energy estimates.
- **Rack-aware network delay**: same-rack and cross-rack latency/bandwidth are configurable and included in topology metrics.
- **VM migration penalties**: migration cost (`$/GB`) and downtime (`ms/GB`) are part of objective outputs.
- **Heterogeneous hardware**: host pools and VM profiles are declared explicitly in config.
- **Network topology awareness**: hosts are grouped by rack and cross-rack placement is penalized using configured network characteristics.
- **Dynamic workload loop**: each run executes multiple simulation time steps and tracks placement transitions.
- **Benchmarks included**: First Fit and Best Fit Decreasing baselines are part of experiment sweeps.
- **Statistical rigor**: experiment sweeps use multiple random seeds and export mean/std summaries.

## Build and Run

Run commands from the project root:

```powershell
cd C:\Users\HP\Desktop\NSBM\cloud-sim\intelligent-vm-optimizer
```

```powershell
mvn clean package
mvn test
mvn -q exec:java "-Dsimulation.config=config/smoke-config.json" "-Dsimulation.algorithms=FIRST_FIT" "-Dsimulation.workloads=VARIABLE" "-Dsimulation.maxSeeds=1"
```

Configuration is loaded from `config/simulation-config.json`.

For the full experiment sweep, use:

```powershell
mvn -q exec:java
```

The full sweep runs every configured algorithm/workload/seed combination and can take much longer than the smoke command.

## Analysis

```powershell
python -m pip install -r analysis/python_analysis/requirements.txt
python analysis/python_analysis/analyze_results.py
``` 

Generated plots include box plots, utilization heatmap, Pareto frontier and convergence trend.
Statistical outputs now also include:
- `results/plots/confidence_intervals.csv` (95% confidence intervals)
- `results/plots/ablation_study.csv` (HYBRID vs PSO_MODIFIED and BEST_FIT deltas/effect sizes)
- `results/plots/anova_report.json` (ANOVA tests for statistical significance)
- `results/plots/summary_stats.csv` (Mean/std per algorithm/workload combination)

## Dataset ingestion
Sample Google/Alibaba traces are under `datasets/`. Parsers:
- `GoogleTraceParser`
- `AlibabaTraceParser`

## Dashboard

```powershell
python -m streamlit run visualization/dashboard/app.py
```

If your terminal is still in the parent `cloud-sim` folder, use:

```powershell
python -m streamlit run intelligent-vm-optimizer/visualization/dashboard/app.py
```

The dashboard now highlights **HYBRID** as the new flagship algorithm and displays measured results from comprehensive multi-algorithm, multi-workload, multi-seed experiments.

## Experimental Results (May 2026)

Comprehensive benchmark suite completed with **45 simulation runs** covering:

**Algorithms (5):**
- HYBRID (NEW - PSO + consolidation)
- PSO_MODIFIED (adaptive PSO)
- PSO_STANDARD (baseline optimization)
- BEST_FIT_DECREASING (sort-and-place)
- BEST_FIT (tight packing)

**Workloads (3):**
- STEADY (predictable demand)
- VARIABLE (dynamic changes)
- BURST (spike-heavy traffic)

**Repetitions:**
- 3 random seeds per combination for statistical rigor

**Key Findings:**
- Utilization difference: HIGHLY SIGNIFICANT (ANOVA p=1.22e-11)
- HYBRID achieves 1.07%-1.33% energy improvement vs BEST_FIT
- Latency improvements vary by workload: -3.71% to +31.39% vs BEST_FIT
- All metrics have 95% confidence bounds documented in `results/plots/confidence_intervals.csv`

**Output Files:**
- 45 detailed simulation results with 24 metrics each
- 9 visualization plots (boxplots, heatmap, Pareto frontier, convergence)
- Statistical reports: ANOVA, confidence intervals, ablation study

## Docker

```bash
docker-compose up --build
```
