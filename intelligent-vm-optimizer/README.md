# Intelligent VM Optimizer

Research-grade simulation framework for intelligent VM scheduling using CloudSim Plus.

## What is modeled now
- **VM migration penalties**: migration cost (`$/GB`) and downtime (`ms/GB`) are part of objective outputs.
- **Heterogeneous hardware**: host pools and VM profiles are declared explicitly in config.
- **Network topology awareness**: hosts are grouped by rack and cross-rack placement is penalized.
- **Dynamic workload loop**: each run executes multiple simulation time steps and tracks placement transitions.
- **Benchmarks included**: First Fit and Best Fit Decreasing baselines are part of experiment sweeps.
- **Statistical rigor**: experiment sweeps use multiple random seeds and export mean/std summaries.

## Build and Run

```bash
mvn clean package
mvn exec:java
```

Configuration is loaded from `config/simulation-config.json`.

## Analysis

```bash
pip install -r analysis/python_analysis/requirements.txt
python analysis/python_analysis/analyze_results.py
```

Generated plots include box plots, utilization heatmap, Pareto frontier and convergence trend.

## Dataset ingestion
Sample Google/Alibaba traces are under `datasets/`. Parsers:
- `GoogleTraceParser`
- `AlibabaTraceParser`

## Dashboard

```bash
streamlit run visualization/dashboard/app.py
```

## Docker

```bash
docker-compose up --build
```
