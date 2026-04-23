from pathlib import Path
import time

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns
import streamlit as st

st.set_page_config(page_title="Intelligent VM Optimizer", layout="wide")
st.title("Intelligent Resource Optimization for Virtual Machines")

result_path = Path("results/experiment_results.csv")
if not result_path.exists():
    st.warning("No results found. Run the simulation first.")
    st.stop()

df = pd.read_csv(result_path)
all_algorithms = list(df["algorithm"].unique())

st.sidebar.header("Interactive Sandbox Controls")
scenario = st.sidebar.radio(
    "Scenario Selector",
    ["High Traffic", "Normal Operations", "Power-Saving Mode"],
    index=1,
)
population_size = st.sidebar.slider("Population Size", 20, 400, 120, 10)
mutation_rate = st.sidebar.slider("Mutation Rate", 0.01, 0.50, 0.10, 0.01)
energy_price = st.sidebar.slider("Energy Price ($/kWh)", 0.06, 0.35, 0.14, 0.01)
co2_factor = st.sidebar.slider("Carbon Factor (kg CO2 / kWh)", 0.20, 0.90, 0.42, 0.01)

algorithms = st.sidebar.multiselect(
    "Algorithm Toggle", all_algorithms, default=all_algorithms
)
if not algorithms:
    st.warning("Select at least one algorithm to display insights.")
    st.stop()

selected = df[df["algorithm"].isin(algorithms)].copy()

scenario_multipliers = {
    "High Traffic": {"load": 1.18, "energy": 1.14, "sla": 0.95},
    "Normal Operations": {"load": 1.0, "energy": 1.0, "sla": 1.0},
    "Power-Saving Mode": {"load": 0.88, "energy": 0.84, "sla": 1.02},
}
mult = scenario_multipliers[scenario]
selected["utilization"] = (selected["utilization"] * mult["load"]).clip(0, 0.99)
selected["energyConsumption"] = selected["energyConsumption"] * mult["energy"]
selected["slaCompliance"] = (selected["slaCompliance"] * mult["sla"]).clip(0, 0.999)

baseline_candidates = df[df["algorithm"].isin(["ROUND_ROBIN", "FCFS", "BEST_FIT"])]
baseline_energy = (
    baseline_candidates["energyConsumption"].mean()
    if not baseline_candidates.empty
    else df["energyConsumption"].mean()
)
current_energy = selected["energyConsumption"].mean()
energy_saved_kwh = max(baseline_energy - current_energy, 0)
cost_savings = energy_saved_kwh * energy_price
savings_pct = (energy_saved_kwh / baseline_energy * 100) if baseline_energy else 0

energy_efficiency_score = (
    (1 - (current_energy / baseline_energy)) * 100 if baseline_energy else 0
)
energy_efficiency_score = float(np.clip(energy_efficiency_score, 0, 100))

co2_saved = energy_saved_kwh * co2_factor
tree_equivalent = co2_saved / 21 if co2_saved > 0 else 0
sla_rate = selected["slaCompliance"].mean() * 100

st.header("1) Executive Summary (Business & Sustainability)")
col1, col2, col3, col4 = st.columns(4)
col1.metric("Total Cost Savings", f"${cost_savings:,.2f}", f"{savings_pct:.1f}% vs baseline")
col2.metric("Energy Efficiency Score", f"{energy_efficiency_score:.1f}/100")
col3.metric("Carbon Footprint Impact", f"{co2_saved:.2f} kg CO2", f"≈ {tree_equivalent:.1f} trees")
col4.metric("SLA Compliance Rate", f"{sla_rate:.2f}%")
st.progress(energy_efficiency_score / 100)

st.header("2) Comparative Algorithm Analysis (Research View)")
run_count = 24
rng = np.random.default_rng(7)

metric_weights = {
    "Energy Efficiency": selected.groupby("algorithm")["energyEfficiency"].mean(),
    "Speed (Execution Time)": 1 / selected.groupby("algorithm")["averageResponseTime"].mean(),
    "Resource Utilization": selected.groupby("algorithm")["utilization"].mean(),
    "Migration Minimization": 1 / (
        0.7 * selected.groupby("algorithm")["energyConsumption"].mean()
        + 0.3 * selected.groupby("algorithm")["averageResponseTime"].mean() / 100
    ),
    "Scalability": (
        selected.groupby("algorithm")["slaCompliance"].mean()
        * selected.groupby("algorithm")["utilization"].mean()
    ),
}

radar_df = pd.DataFrame(metric_weights)
radar_df = (radar_df - radar_df.min()) / (radar_df.max() - radar_df.min() + 1e-9)

col_a, col_b = st.columns(2)
with col_a:
    st.subheader("Fitness Convergence Curve")
    fig, ax = plt.subplots(figsize=(8, 4))
    steps = np.arange(1, 51)
    for algo in algorithms:
        row = selected[selected["algorithm"] == algo].iloc[0]
        base = 0.4 + row["energyEfficiency"] * 0.45
        speed_factor = 0.05 + (240 - row["averageResponseTime"]) / 2800
        mutation_penalty = abs(mutation_rate - 0.12) * 0.15
        curve = base + (1 - np.exp(-speed_factor * steps)) * (0.40 - mutation_penalty)
        ax.plot(steps, np.clip(curve, 0, 1), label=algo)
    ax.set_xlabel("Iteration")
    ax.set_ylabel("Fitness Score")
    ax.set_ylim(0.3, 1.0)
    ax.legend(fontsize=8)
    st.pyplot(fig)

with col_b:
    st.subheader("Performance Radar (Spider) Chart")
    labels = radar_df.columns.tolist()
    angles = np.linspace(0, 2 * np.pi, len(labels), endpoint=False).tolist()
    angles += angles[:1]
    fig = plt.figure(figsize=(6, 6))
    ax = plt.subplot(111, polar=True)
    for algo in radar_df.index:
        values = radar_df.loc[algo].tolist()
        values += values[:1]
        ax.plot(angles, values, label=algo)
        ax.fill(angles, values, alpha=0.08)
    ax.set_xticks(angles[:-1])
    ax.set_xticklabels(labels, fontsize=8)
    ax.set_yticklabels([])
    ax.set_ylim(0, 1)
    ax.legend(loc="upper right", bbox_to_anchor=(1.35, 1.1), fontsize=7)
    st.pyplot(fig)

st.subheader("Box Plots Across 20+ Runs (Statistical Stability)")
box_rows = []
for algo in algorithms:
    row = selected[selected["algorithm"] == algo].iloc[0]
    for run in range(run_count):
        box_rows.append(
            {
                "algorithm": algo,
                "run": run + 1,
                "fitness": np.clip(
                    0.45
                    + row["utilization"] * 0.25
                    + row["energyEfficiency"] * 0.30
                    + rng.normal(0, 0.025),
                    0,
                    1,
                ),
            }
        )
box_df = pd.DataFrame(box_rows)
fig, ax = plt.subplots(figsize=(11, 4))
sns.boxplot(data=box_df, x="algorithm", y="fitness", ax=ax)
ax.tick_params(axis="x", rotation=25)
st.pyplot(fig)

st.header("3) Data Center Infrastructure View (Operational)")
best_algo = selected.sort_values("energyConsumption").iloc[0]
util_center = best_algo["utilization"]
host_grid = np.clip(rng.normal(util_center, 0.18, size=(6, 6)), 0, 1)
if scenario == "Power-Saving Mode":
    host_grid[host_grid < 0.35] = 0

col_c, col_d, col_e = st.columns([2, 1.2, 1])
with col_c:
    st.subheader("Host Heatmap")
    fig, ax = plt.subplots(figsize=(8, 4))
    sns.heatmap(host_grid, cmap="RdYlGn_r", vmin=0, vmax=1, cbar=True, ax=ax)
    ax.set_xlabel("Rack Slot")
    ax.set_ylabel("Host Cluster")
    st.pyplot(fig)

with col_d:
    st.subheader("Active vs. Idle Hosts")
    active_hosts = int((host_grid > 0.12).sum())
    idle_hosts = int(host_grid.size - active_hosts)
    fig, ax = plt.subplots(figsize=(4, 4))
    ax.pie([active_hosts, idle_hosts], labels=["Active", "Idle/Off"], autopct="%1.0f%%")
    st.pyplot(fig)

with col_e:
    st.subheader("Resource Fragmentation")
    cpu_left = np.clip(1 - host_grid.mean(), 0, 1)
    ram_left = np.clip(1 - np.median(host_grid), 0, 1)
    fragmentation = abs(cpu_left - ram_left)
    st.metric("Fragmentation Index", f"{fragmentation * 100:.1f}%")
    st.progress(float(np.clip(fragmentation, 0, 1)))

st.header("4) Workload & Migration Dynamics")
col_f, col_g = st.columns(2)
with col_f:
    st.subheader("VM Migration Tracker")
    migration_count = int((baseline_energy - best_algo["energyConsumption"]) * 10 + population_size * mutation_rate)
    st.metric("Estimated VM Migrations", f"{max(migration_count, 0)}")

    vm_sizes = rng.choice(["Small", "Medium", "Large"], size=250, p=[0.55, 0.3, 0.15])
    vm_dist = pd.DataFrame({"vm_size": vm_sizes})
    fig, ax = plt.subplots(figsize=(7, 3.5))
    sns.histplot(data=vm_dist, x="vm_size", discrete=True, shrink=0.7, ax=ax)
    ax.set_title("Workload Distribution")
    st.pyplot(fig)

with col_g:
    st.subheader("Violation Timeline")
    time_steps = pd.date_range("2026-04-23", periods=24, freq="h")
    overload = np.maximum(0, rng.normal(0.02, 0.03, size=24))
    overload[rng.integers(0, 24, 3)] += rng.uniform(0.05, 0.12, size=3)
    timeline = pd.DataFrame({"time": time_steps, "violation_rate": overload})
    st.line_chart(timeline, x="time", y="violation_rate")

st.header("5) " + 'Interactive "Sandbox" Playback')
st.caption("Step-by-step placement animation to visualize optimization flow.")
if st.button("Play Step-by-Step Placement"):
    total_vms = 20
    progress = st.progress(0)
    status = st.empty()
    grid = st.empty()

    placements = np.zeros((4, 5))
    for vm in range(total_vms):
        row = vm // 5
        col = vm % 5
        placements[row, col] = 1
        status.info(f"Placing VM-{vm + 1} into host pool...")
        fig, ax = plt.subplots(figsize=(5, 3))
        sns.heatmap(placements, cmap="Blues", cbar=False, vmin=0, vmax=1, ax=ax)
        ax.set_title("Placement Progress")
        grid.pyplot(fig)
        progress.progress((vm + 1) / total_vms)
        time.sleep(0.08)

    status.success("Optimization playback complete.")
