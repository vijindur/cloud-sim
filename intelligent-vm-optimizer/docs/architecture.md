# Intelligent Resource Optimization for Virtual Machines in Cloud Environments

## System Architecture

The platform is organized into modular components:

- **core**: CloudSim Plus infrastructure and orchestration.
- **scheduler**: Baseline and hybrid scheduling strategies.
- **pso**: Standard and modified PSO implementations.
- **decision**: Adaptive engine for dynamic algorithm selection.
- **energy**: Host-level and datacenter-level energy estimation.
- **workload**: Workload characterization and trace parsing.
- **metrics**: Unified output models and exporters.
- **experiments**: Automated execution across algorithms and workloads.
- **analysis/python_analysis**: Statistical and visual post-processing.
- **visualization/dashboard**: Streamlit-based interactive dashboard.

## Hybrid Scheduler

The hybrid scheduler combines **Best Fit** and **Modified PSO** with adaptive switching:

- Low load + low variability → heuristic routing.
- High load or high variability → optimization routing.
- Mid-range behavior → hybrid fallback.

## Modified PSO Design

Modified PSO uses:

- Population size = 20
- Maximum iterations = 15
- Early stopping on marginal fitness improvement

### Fitness Function

\[
Fitness = 0.40\times U + 0.35\times SLA + 0.25\times E
\]

Where:
- \(U\): resource utilization
- \(SLA\): SLA compliance
- \(E\): energy efficiency

## Experimental Design

- Algorithms: RR, FCFS, Best Fit, Standard PSO, Modified PSO, Hybrid.
- Workloads: steady, variable, burst.
- Repetitions: 30 per algorithm/workload pair.
- Outputs: CSV + JSON metrics and ANOVA reports.
