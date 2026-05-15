# Intelligent Resource Optimization for Virtual Machines in Cloud Environments

## 1) Project Overview
A research-oriented, production-leaning cloud optimization platform that couples **CloudSim Plus simulation**, **workload-aware scheduling**, and an **interactive analytics dashboard**.

## 2) Problem Statement
Traditional VM schedulers optimize one objective at a time and respond poorly to shifting workload pressure (bursty, CPU-heavy, latency-sensitive), causing energy waste, SLA violations, and unstable response time.

## 3) Research Motivation
We need reproducible, metrics-driven evaluation of multi-objective schedulers using real workload traces, not static demos.

## 4) Objectives
- Optimize utilization, response time, SLA compliance, migration cost, and energy.
- Ensure workload type + metric priorities change actual outcomes.
- Provide reproducible simulation and explainable visual analytics.

## 5) System Architecture
Dataset → Parser → Workload Profiler → Scheduler Engine → CloudSim Execution → Metrics Export → Dashboard.

Details: `docs/architecture.md`.

## 6) Features
- CloudSim Plus simulation with config-driven hosts/VMs.
- Multiple algorithms: FCFS, First Fit, Best Fit, BFD, PSO, Modified PSO, HYBRID.
- Workload-aware request selection and normalization.
- Admission control, migration penalties, topology/network effects, host energy models.
- Streamlit dashboard with interactive filtering and walkthrough.

## 7) Technologies Used
- Java 17+, Maven, CloudSim Plus
- Python (Streamlit, Plotly, Pandas)
- Docker / Docker Compose

## 8) Algorithms Implemented
See `src/main/java/com/cloudoptimizer/scheduler` and `src/main/java/com/cloudoptimizer/pso`.

## 9) Workload Intelligence
Workload parsing and type-aware selection are implemented in `src/main/java/com/cloudoptimizer/workload` and integrated in `SimulationOrchestrator`.

## 10) Dataset Information
Primary dataset file:
- `datasets/cloud_workload_dataset.csv`

Parsers included for additional traces:
- `GoogleTraceParser`
- `AlibabaTraceParser`

## 11) Real Metrics
All dashboard charts are driven by exported simulation results (`results/experiment_results.csv` / `.json`).

## 12) Screenshots/Diagrams
- Add architecture diagram screenshot here.
- Add dashboard overview screenshot here.

## 13) Installation
```bash
git clone <repo>
cd intelligent-vm-optimizer
```

## 14) Docker Setup
```bash
docker compose up --build
```

## 15) Local Development
```bash
mvn -q exec:java
python -m pip install -r analysis/python_analysis/requirements.txt
python -m streamlit run visualization/dashboard/app.py
```

## 16) Run Simulations
```bash
mvn -q exec:java "-Dsimulation.config=config/simulation-config.json"
```

## 17) Switch Schedulers
```bash
mvn -q exec:java "-Dsimulation.algorithms=HYBRID,BEST_FIT,PSO_MODIFIED"
```

## 18) Test Workload Types
```bash
mvn -q exec:java "-Dsimulation.workloads=STEADY,VARIABLE,BURST"
```

## 19) API Documentation
Current project is simulation + dashboard centric (no standalone REST API contract yet).

## 20) Folder Structure
- `src/main/java`: core simulation, schedulers, workload, metrics
- `config`: simulation configs
- `datasets`: workload traces
- `results`: simulation outputs
- `analysis/python_analysis`: analytics scripts
- `visualization/dashboard`: Streamlit UI

## 21) Performance Results
Generated after experiment runs and summarized in:
- `results/experiment_summary.csv`
- `results/experiment_results.csv`

## 22) Benchmark Comparisons
Run multi-algorithm sweeps and compare in dashboard filters and analysis scripts.

## 23) Achievements
- End-to-end dataset→simulation→metrics→dashboard pipeline
- Multi-objective outputs with scheduler-dependent divergence
- Interactive visualization for comparative decision-making

## 24) Current Limitations
- Maven dependency fetch may fail in restricted networks.
- Walkthrough uses Streamlit state animation (not full Framer/GSAP web stack).
- External trace ingestion beyond bundled dataset requires format alignment.

## 25) Future Improvements
- Add REST service for simulation control.
- Add RL-based scheduler and online learning loop.
- Add richer real-time event streaming charts.

## 26) Troubleshooting
- If Java run fails, verify JDK/Maven versions and internet access for dependencies.
- If dashboard appears blank, ensure `results/experiment_results.csv` exists.

## 27) Deployment Instructions
1. Build and validate simulation outputs.
2. Start dashboard via Docker Compose.
3. Verify charts update with changed algorithm/workload filters.

## 28) Contribution Guidelines
- Use feature branches.
- Add tests for scheduler/metrics logic changes.
- Keep configs/data assumptions explicit and reproducible.
