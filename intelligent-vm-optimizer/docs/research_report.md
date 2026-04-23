# Research Report: Intelligent VM Optimizer

## Objective
Evaluate baseline schedulers against standard and modified PSO strategies for cloud VM task mapping.

## Experimental Design
- **Algorithms:** Round Robin, FCFS, Best Fit, PSO Standard, PSO Modified, Hybrid.
- **Workloads:** STEADY, VARIABLE, BURST.
- **Replications:** 30 per (algorithm, workload) pair.
- **Total runs:** 6 x 3 x 30 = **540 runs**.

## Modified PSO contribution
The modified PSO extends the standard PSO by adding:
1. **Inertia weight decay** from 0.9 to 0.4 over iterations.
2. **Velocity clamping** with a stricter bound than standard PSO.
3. **Early stopping** based on convergence threshold across outer candidate loops.

Rationale:
- Inertia decay encourages global search in early steps and local refinement later.
- Velocity clamping reduces oscillation and stabilizes mapping assignments.
- Early stopping avoids unnecessary computation when improvement stalls.

## Metrics
- Utilization
- SLA compliance (load-balance-derived)
- Energy efficiency
- Total energy consumption
- Average response time

## Statistical Analysis
ANOVA is applied per metric across algorithms. Boxplots are generated for all primary metrics under `results/plots/`.

## Data Sources
- Local expanded sample traces under `datasets/google_trace` and `datasets/alibaba_trace`.
- Public source references documented in `docs/trace_sources.md`.

## Reproducibility
1. `mvn -q test`
2. `mvn -q exec:java`
3. `python3 analysis/python_analysis/analyze_results.py`

Outputs:
- `results/experiment_results.csv`
- `results/experiment_results.json`
- `results/plots/*.svg`
- `results/plots/anova_report.json`
