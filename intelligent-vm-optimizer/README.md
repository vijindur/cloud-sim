# Intelligent VM Optimizer

Research-grade simulation framework for intelligent VM scheduling using CloudSim Plus.

## Build and Run

```bash
mvn clean package
mvn exec:java
```

The full experiment suite executes **540 runs** (6 algorithms x 3 workloads x 30 repetitions)
and exports `results/experiment_results.csv` and `results/experiment_results.json`.

## Analysis

```bash
python3 analysis/python_analysis/analyze_results.py
```

The analysis step generates ANOVA output and plots in `results/plots/`.

## Tests

```bash
mvn test
```

JUnit tests live under `src/test/` and cover PSO fitness behavior and scheduler mapping correctness.

## Modified PSO features

`PSO_MODIFIED` includes:
- inertia weight decay,
- velocity clamping,
- convergence-based early stopping.

See `docs/research_report.md` for details and motivation.

## Datasets

Expanded sample traces (400 rows each) are included in:
- `datasets/google_trace/sample_google_trace.csv`
- `datasets/alibaba_trace/sample_alibaba_trace.csv`

Real trace source references are documented in `docs/trace_sources.md`.

## Dashboard

```bash
streamlit run visualization/dashboard/app.py
```

## Docker

```bash
docker-compose up --build
```
