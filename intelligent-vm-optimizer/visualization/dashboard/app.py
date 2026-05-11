from pathlib import Path
import sys
import time

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns
import streamlit as st

PROJECT_ROOT = Path(__file__).resolve().parents[2]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from analysis.python_analysis.data_loader import load_cloud_workload_dataset


st.set_page_config(
    page_title="Intelligent VM Optimizer",
    layout="wide",
    initial_sidebar_state="expanded",
)

sns.set_theme(style="whitegrid")

RESULTS_PATH = PROJECT_ROOT / "results" / "experiment_results.csv"
WORKLOAD_PATH = PROJECT_ROOT / "datasets" / "cloud_workload_dataset.csv"
LEGACY_ALGOS = ["ROUND_ROBIN", "FCFS", "BEST_FIT"]
MODERN_ALGOS = ["PSO_STANDARD", "PSO_MODIFIED", "HYBRID"]
ALGO_PROFILES = {
    "ROUND_ROBIN": {
        "energy": 1.12,
        "latency": 1.12,
        "sla": 0.92,
        "placement": 0.30,
        "balance": 0.40,
        "label": "Simple rotation",
    },
    "FCFS": {
        "energy": 1.15,
        "latency": 1.18,
        "sla": 0.90,
        "placement": 0.22,
        "balance": 0.32,
        "label": "Arrival-order placement",
    },
    "BEST_FIT": {
        "energy": 1.03,
        "latency": 1.02,
        "sla": 0.97,
        "placement": 0.58,
        "balance": 0.62,
        "label": "Tight packing",
    },
    "PSO_STANDARD": {
        "energy": 0.94,
        "latency": 0.92,
        "sla": 1.02,
        "placement": 0.78,
        "balance": 0.74,
        "label": "Particle-guided search",
    },
    "PSO_MODIFIED": {
        "energy": 0.89,
        "latency": 0.87,
        "sla": 1.06,
        "placement": 0.86,
        "balance": 0.81,
        "label": "Adaptive PSO",
    },
    "HYBRID": {
        "energy": 0.84,
        "latency": 0.81,
        "sla": 1.09,
        "placement": 0.93,
        "balance": 0.89,
        "label": "PSO + consolidation",
    },
}
SCENARIO_PROFILES = {
    "Balanced Operations": {"energy": 1.00, "latency": 1.00, "sla": 1.00, "util": 1.00},
    "Peak Demand Surge": {"energy": 1.12, "latency": 1.20, "sla": 0.96, "util": 1.08},
    "Efficiency First": {"energy": 0.91, "latency": 1.04, "sla": 1.01, "util": 0.95},
}


def build_measured_summary(results_lookup: pd.DataFrame, algorithms: list[str]) -> pd.DataFrame:
    available = [algo for algo in algorithms if algo in results_lookup.index]
    if not available:
        return pd.DataFrame()

    summary = results_lookup.loc[available].reset_index().copy()
    summary = summary[
        [
            "algorithm",
            "workload",
            "energyConsumption",
            "averageResponseTime",
            "slaCompliance",
            "utilization",
            "energyEfficiency",
        ]
    ]
    summary["energy_score"] = min_max_scale(summary["energyConsumption"], invert=True)
    summary["latency_score"] = min_max_scale(summary["averageResponseTime"], invert=True)
    summary["sla_score"] = min_max_scale(summary["slaCompliance"])
    summary["util_score"] = min_max_scale(summary["utilization"])
    summary["eff_score"] = min_max_scale(summary["energyEfficiency"])
    summary["compositeScore"] = (
        summary["energy_score"] + summary["latency_score"] + summary["sla_score"] + summary["util_score"] + summary["eff_score"]
    ) / 5 * 100
    summary["rank"] = summary["compositeScore"].rank(ascending=False, method="dense").astype(int)
    return summary.sort_values(["rank", "energyConsumption"]).reset_index(drop=True)


@st.cache_data(show_spinner=False)
def load_results() -> pd.DataFrame:
    df = pd.read_csv(RESULTS_PATH)
    df["algorithm"] = df["algorithm"].str.upper()
    return df.set_index("algorithm")


@st.cache_data(show_spinner=False)
def load_workload() -> pd.DataFrame:
    df = load_cloud_workload_dataset(WORKLOAD_PATH)
    df["submit_date"] = df["submit_time"].dt.date
    df["submit_hour"] = df["submit_time"].dt.hour
    df["weekday"] = df["submit_time"].dt.day_name()
    df["queue_wait_minutes"] = df["queue_wait_seconds"] / 60
    df["memory_requested_gb"] = df["memory_requested_mb"] / 1024
    df["memory_used_gb"] = df["memory_used_mb"] / 1024
    df["cpu_ratio"] = (df["cpu_used"] / df["cpu_requested"]).replace([np.inf, -np.inf], 0).fillna(0).clip(0, 1)
    df["memory_ratio"] = (
        (df["memory_used_mb"] / df["memory_requested_mb"]).replace([np.inf, -np.inf], 0).fillna(0).clip(0, 1)
    )
    return df


def inject_styles() -> None:
    st.markdown(
        """
        <style>
        .stApp {
            background:
                radial-gradient(circle at top left, rgba(22, 63, 60, 0.18), transparent 28%),
                radial-gradient(circle at top right, rgba(224, 122, 63, 0.14), transparent 24%),
                linear-gradient(180deg, #f7f2e9 0%, #f9faf8 40%, #edf4f2 100%);
        }
        [data-testid="stSidebar"] {
            background: linear-gradient(180deg, #102a2a 0%, #1a4442 100%);
        }
        [data-testid="stSidebar"] * {
            color: #f6f0e3 !important;
        }
        .hero-card, .glass-card {
            border: 1px solid rgba(16, 42, 42, 0.08);
            border-radius: 24px;
            background: rgba(255, 252, 246, 0.82);
            box-shadow: 0 18px 40px rgba(16, 42, 42, 0.09);
            backdrop-filter: blur(10px);
            animation: rise 0.6s ease;
        }
        .hero-card { padding: 1.6rem; }
        .glass-card { padding: 1rem 1.1rem; height: 100%; }
        .eyebrow {
            display: inline-block;
            padding: 0.3rem 0.7rem;
            border-radius: 999px;
            background: #163f3c;
            color: #f7f3ea;
            font-size: 0.78rem;
            letter-spacing: 0.08em;
            text-transform: uppercase;
        }
        .hero-title {
            color: #102a2a;
            font-size: 2.5rem;
            font-weight: 800;
            line-height: 1.05;
            margin: 0.8rem 0 0.45rem 0;
        }
        .hero-copy, .section-copy {
            color: #45615c;
            line-height: 1.65;
        }
        .section-title {
            color: #102a2a;
            font-size: 1.28rem;
            font-weight: 800;
            margin-bottom: 0.25rem;
        }
        .metric-label {
            color: #5b6e69;
            font-size: 0.8rem;
            text-transform: uppercase;
            letter-spacing: 0.08em;
        }
        .metric-value {
            color: #102a2a;
            font-size: 1.95rem;
            font-weight: 800;
            margin-top: 0.2rem;
        }
        .metric-note {
            color: #cf6d33;
            margin-top: 0.25rem;
            font-size: 0.92rem;
        }
        .leader {
            border-left: 4px solid #e07a3f;
            padding-left: 0.9rem;
            margin-top: 0.55rem;
        }
        .callout {
            border-radius: 18px;
            background: linear-gradient(135deg, rgba(22,63,60,0.96), rgba(36,88,84,0.92));
            color: #f7f3ea;
            padding: 1rem 1.1rem;
            animation: pulseIn 0.7s ease;
        }
        @keyframes rise {
            from { opacity: 0; transform: translateY(10px); }
            to { opacity: 1; transform: translateY(0); }
        }
        @keyframes pulseIn {
            from { opacity: 0; transform: scale(0.98); }
            to { opacity: 1; transform: scale(1); }
        }
        </style>
        """,
        unsafe_allow_html=True,
    )


def section_heading(title: str, copy: str) -> None:
    st.markdown(
        f"""
        <div class="glass-card">
            <div class="section-title">{title}</div>
            <div class="section-copy">{copy}</div>
        </div>
        """,
        unsafe_allow_html=True,
    )


def metric_card(label: str, value: str, note: str) -> None:
    st.markdown(
        f"""
        <div class="glass-card">
            <div class="metric-label">{label}</div>
            <div class="metric-value">{value}</div>
            <div class="metric-note">{note}</div>
        </div>
        """,
        unsafe_allow_html=True,
    )


def min_max_scale(series: pd.Series, invert: bool = False) -> pd.Series:
    span = series.max() - series.min()
    if span <= 1e-9:
        scaled = pd.Series(1.0, index=series.index)
    else:
        scaled = (series - series.min()) / span
    return 1 - scaled if invert else scaled


def radar_frame(summary_df: pd.DataFrame) -> pd.DataFrame:
    cols = ["energy_score", "latency_score", "sla_score", "util_score", "eff_score"]
    return summary_df[["algorithm", *cols]].melt("algorithm", var_name="dimension", value_name="score")


def simulate_algorithm_metrics(
    workload: pd.DataFrame,
    results_lookup: pd.DataFrame,
    algorithms: list[str],
    scenario_name: str,
    host_count: int,
    host_cpu_capacity: int,
    host_memory_capacity: int,
    energy_weight: float,
    latency_weight: float,
    sla_weight: float,
) -> tuple[pd.DataFrame, pd.DataFrame, float]:
    workload_stats = workload.copy()
    workload_stats["cpu_norm"] = workload_stats["cpu_requested"] / max(workload_stats["cpu_requested"].max(), 1)
    workload_stats["memory_norm"] = workload_stats["memory_requested_gb"] / max(workload_stats["memory_requested_gb"].max(), 1)
    workload_stats["duration_norm"] = workload_stats["duration"] / max(workload_stats["duration"].max(), 1)
    workload_stats["wait_norm"] = workload_stats["queue_wait_seconds"] / max(workload_stats["queue_wait_seconds"].max(), 1)

    scenario = SCENARIO_PROFILES[scenario_name]
    cluster_pressure = (
        workload_stats["cpu_requested"].sum() / max(host_count * host_cpu_capacity, 1)
        + workload_stats["memory_requested_gb"].sum() / max(host_count * host_memory_capacity, 1)
    ) / 2
    cluster_pressure = float(np.clip(cluster_pressure, 0.55, 1.45))

    rows: list[dict] = []
    job_rows: list[dict] = []
    for algorithm in algorithms:
        if algorithm not in ALGO_PROFILES or algorithm not in results_lookup.index:
            continue

        profile = ALGO_PROFILES[algorithm]
        baseline = results_lookup.loc[algorithm]
        placement_quality = profile["placement"]
        balance_quality = profile["balance"]

        energy_multiplier = (
            profile["energy"]
            * scenario["energy"]
            * (1 + max(cluster_pressure - 1, 0) * (1.16 - placement_quality))
            * (1.08 - energy_weight * 0.22)
        )
        latency_multiplier = (
            profile["latency"]
            * scenario["latency"]
            * (1 + max(cluster_pressure - 1, 0) * (1.20 - balance_quality))
            * (1.08 - latency_weight * 0.20)
        )
        sla_multiplier = (
            profile["sla"]
            * scenario["sla"]
            * (0.98 + sla_weight * 0.12)
            * (1 - max(cluster_pressure - 1, 0) * (0.09 - placement_quality * 0.04))
        )
        util_multiplier = scenario["util"] * (0.90 + placement_quality * 0.13 + balance_quality * 0.05)

        job_energy = (
            2.1
            + workload_stats["cpu_requested"] * 0.36
            + workload_stats["memory_requested_gb"] * 0.055
            + workload_stats["queue_wait_minutes"] * 0.045
            + workload_stats["node_count"] * 0.14
        ) * energy_multiplier
        job_latency = (
            workload_stats["queue_wait_seconds"] * 0.48
            + workload_stats["duration"] * 0.065
            + workload_stats["cpu_requested"] * 5.8
        ) * latency_multiplier
        job_sla = (
            0.78
            + placement_quality * 0.12
            + balance_quality * 0.06
            - workload_stats["wait_norm"] * 0.09
            - workload_stats["cpu_norm"] * 0.04
        ) * sla_multiplier
        job_sla = job_sla.clip(0.70, 0.999)

        job_util = (
            0.56
            + workload_stats["cpu_norm"] * 0.16
            + workload_stats["memory_norm"] * 0.10
            + placement_quality * 0.11
            + balance_quality * 0.06
        ) * util_multiplier
        job_util = job_util.clip(0.45, 0.99)
        job_eff = (1 - (job_energy / max(job_energy.max(), 1))) * 0.42 + placement_quality * 0.58
        job_eff = job_eff.clip(0.50, 0.99)

        job_rows.extend(
            pd.DataFrame(
                {
                    "algorithm": algorithm,
                    "job_id": workload_stats["job_id"].values,
                    "submit_time": workload_stats["submit_time"].values,
                    "job_type": workload_stats["job_type"].values,
                    "priority_level": workload_stats["priority_level"].values,
                    "energyConsumption": job_energy.values,
                    "averageResponseTime": job_latency.values,
                    "slaCompliance": job_sla.values,
                    "utilization": job_util.values,
                    "energyEfficiency": job_eff.values,
                }
            ).to_dict("records")
        )

        rows.append(
            {
                "algorithm": algorithm,
                "workload": "CLOUD_DATASET",
                "energyConsumption": float(job_energy.mean() * (baseline["energyConsumption"] / 10.5)),
                "averageResponseTime": float(job_latency.mean() * (baseline["averageResponseTime"] / 190)),
                "slaCompliance": float(job_sla.mean() * (baseline["slaCompliance"] / 0.86)),
                "utilization": float(job_util.mean() * (baseline["utilization"] / 0.79)),
                "energyEfficiency": float(job_eff.mean() * (baseline["energyEfficiency"] / 0.74)),
            }
        )

    summary = pd.DataFrame(rows)
    per_job = pd.DataFrame(job_rows)

    summary["slaCompliance"] = summary["slaCompliance"].clip(0.72, 0.999)
    summary["utilization"] = summary["utilization"].clip(0.50, 0.99)
    summary["energyEfficiency"] = summary["energyEfficiency"].clip(0.50, 0.99)
    summary["energy_score"] = min_max_scale(summary["energyConsumption"], invert=True)
    summary["latency_score"] = min_max_scale(summary["averageResponseTime"], invert=True)
    summary["sla_score"] = min_max_scale(summary["slaCompliance"])
    summary["util_score"] = min_max_scale(summary["utilization"])
    summary["eff_score"] = min_max_scale(summary["energyEfficiency"])

    total_weight = energy_weight + latency_weight + sla_weight + 0.6 + 0.4
    summary["compositeScore"] = (
        summary["energy_score"] * energy_weight
        + summary["latency_score"] * latency_weight
        + summary["sla_score"] * sla_weight
        + summary["util_score"] * 0.6
        + summary["eff_score"] * 0.4
    ) / total_weight * 100
    summary["rank"] = summary["compositeScore"].rank(ascending=False, method="dense").astype(int)
    summary = summary.sort_values(["rank", "energyConsumption"]).reset_index(drop=True)
    return summary, per_job, cluster_pressure


def choose_host_scores(
    state_cpu: np.ndarray,
    state_mem: np.ndarray,
    cpu_demand: float,
    mem_demand: float,
    algorithm: str,
    host_cpu_capacity: int,
    host_memory_capacity: int,
    step_index: int,
) -> np.ndarray:
    profile = ALGO_PROFILES[algorithm]
    cpu_free = 1 - state_cpu / host_cpu_capacity
    mem_free = 1 - state_mem / host_memory_capacity
    demand_ratio = min(cpu_demand / host_cpu_capacity, 1) * 0.65 + min(mem_demand / host_memory_capacity, 1) * 0.35

    if algorithm == "ROUND_ROBIN":
        preferred = step_index % len(state_cpu)
        scores = np.full(len(state_cpu), 0.1)
        scores[preferred] = 1.0
    elif algorithm == "FCFS":
        scores = 1 - np.arange(len(state_cpu)) / max(len(state_cpu) - 1, 1)
    elif algorithm == "BEST_FIT":
        scores = 1 - np.abs((cpu_free + mem_free) / 2 - demand_ratio)
    else:
        fit = 1 - np.abs(cpu_free - demand_ratio)
        balance = 1 - np.abs(cpu_free - mem_free)
        headroom = (cpu_free * 0.6 + mem_free * 0.4)
        scores = (
            fit * (0.35 + profile["placement"] * 0.15)
            + balance * (0.20 + profile["balance"] * 0.20)
            + headroom * (0.18 + profile["placement"] * 0.12)
        )
        if algorithm == "HYBRID":
            consolidation = 1 - np.abs(cpu_free - 0.35)
            scores += consolidation * 0.28
        elif algorithm == "PSO_MODIFIED":
            scores += (1 - np.abs(mem_free - 0.45)) * 0.18
        elif algorithm == "PSO_STANDARD":
            scores += (1 - np.abs(cpu_free - 0.50)) * 0.12

    feasible = (state_cpu + cpu_demand <= host_cpu_capacity) & (state_mem + mem_demand <= host_memory_capacity)
    scores = np.where(feasible, scores, -1e9)
    return scores


def render_algorithm_animation(
    workload: pd.DataFrame,
    algorithm: str,
    host_count: int,
    host_cpu_capacity: int,
    host_memory_capacity: int,
    frame_delay: float,
) -> None:
    sample_jobs = (
        workload.sort_values(["priority_level", "submit_time"], ascending=[True, True])
        .head(18)[["job_id", "job_type", "cpu_requested", "memory_requested_gb", "priority_level"]]
        .reset_index(drop=True)
    )
    host_cpu = np.zeros(host_count, dtype=float)
    host_mem = np.zeros(host_count, dtype=float)
    placements: list[dict] = []

    progress = st.progress(0)
    summary = st.empty()
    scores_placeholder = st.empty()
    cluster_placeholder = st.empty()
    table_placeholder = st.empty()

    for step, row in sample_jobs.iterrows():
        scores = choose_host_scores(
            host_cpu,
            host_mem,
            float(row["cpu_requested"]),
            float(row["memory_requested_gb"]),
            algorithm,
            host_cpu_capacity,
            host_memory_capacity,
            step,
        )
        chosen_host = int(np.argmax(scores))
        host_cpu[chosen_host] += float(row["cpu_requested"])
        host_mem[chosen_host] += float(row["memory_requested_gb"])
        placements.append(
            {
                "job_id": row["job_id"],
                "type": row["job_type"],
                "priority": row["priority_level"],
                "host": chosen_host + 1,
                "cpu": int(row["cpu_requested"]),
                "memory_gb": float(row["memory_requested_gb"]),
            }
        )

        summary.markdown(
            f"""
            <div class="callout">
                <strong>Step {step + 1}:</strong> {algorithm} placed <strong>{row['job_id']}</strong>
                on host <strong>H{chosen_host + 1}</strong>.
                CPU demand: <strong>{int(row['cpu_requested'])}</strong>,
                memory demand: <strong>{row['memory_requested_gb']:.1f} GB</strong>.
            </div>
            """,
            unsafe_allow_html=True,
        )

        fig_scores, ax_scores = plt.subplots(figsize=(8.2, 3.2))
        score_df = pd.DataFrame({"Host": [f"H{i+1}" for i in range(host_count)], "Score": scores})
        sns.barplot(data=score_df, x="Host", y="Score", color="#e07a3f", ax=ax_scores)
        ax_scores.set_title(f"{algorithm} host scores for {row['job_id']}")
        ax_scores.tick_params(axis="x", rotation=0)
        scores_placeholder.pyplot(fig_scores, use_container_width=True)

        fig_cluster, ax_cluster = plt.subplots(figsize=(8.2, 3.6))
        cluster_df = pd.DataFrame(
            {
                "Host": [f"H{i+1}" for i in range(host_count)],
                "CPU Load %": host_cpu / host_cpu_capacity * 100,
                "Memory Load %": host_mem / host_memory_capacity * 100,
            }
        )
        cluster_melt = cluster_df.melt("Host", var_name="Metric", value_name="Load")
        sns.barplot(data=cluster_melt, x="Host", y="Load", hue="Metric", ax=ax_cluster)
        ax_cluster.set_ylim(0, 100)
        ax_cluster.set_title("Cluster state after placement")
        cluster_placeholder.pyplot(fig_cluster, use_container_width=True)

        table_placeholder.dataframe(pd.DataFrame(placements), use_container_width=True, height=240)
        progress.progress((step + 1) / len(sample_jobs))
        time.sleep(frame_delay)

    summary.markdown(
        f"""
        <div class="callout">
            {algorithm} finished scheduling {len(sample_jobs)} jobs.
            The animation shows how host scoring, balance, and consolidation choices evolve step by step.
        </div>
        """,
        unsafe_allow_html=True,
    )


inject_styles()

if not RESULTS_PATH.exists():
    st.warning("No experiment results found. Run the simulation first.")
    st.stop()

if not WORKLOAD_PATH.exists():
    st.warning("`datasets/cloud_workload_dataset.csv` was not found.")
    st.stop()

results_lookup = load_results()
workload_df = load_workload()

st.sidebar.markdown("## Dashboard Controls")
metric_mode = st.sidebar.radio(
    "Metric source",
    ["Projection (what-if)", "Measured (experiment CSV)"],
    help="Projection uses scenario multipliers over filtered workload. Measured reads aggregate experiment outputs directly.",
)
scenario = st.sidebar.selectbox("Operating posture", list(SCENARIO_PROFILES))

min_date = workload_df["submit_date"].min()
max_date = workload_df["submit_date"].max()
date_range = st.sidebar.date_input(
    "Submission window",
    value=(min_date, max_date),
    min_value=min_date,
    max_value=max_date,
)
if isinstance(date_range, tuple) and len(date_range) == 2:
    start_date, end_date = date_range
else:
    start_date = end_date = date_range

job_types = st.sidebar.multiselect(
    "Job types",
    sorted(workload_df["job_type"].dropna().unique().tolist()),
    default=sorted(workload_df["job_type"].dropna().unique().tolist()),
)
priorities = st.sidebar.multiselect(
    "Priority levels",
    sorted(workload_df["priority_level"].dropna().unique().tolist()),
    default=sorted(workload_df["priority_level"].dropna().unique().tolist()),
)
algorithms = st.sidebar.multiselect(
    "Algorithms in comparison",
    results_lookup.index.tolist(),
    default=results_lookup.index.tolist(),
)
host_count = st.sidebar.slider("Active hosts", 6, 24, 12, 2)
host_cpu_capacity = st.sidebar.slider("CPU per host", 8, 48, 24, 4)
host_memory_capacity = st.sidebar.slider("Memory per host (GB)", 16, 128, 64, 8)
st.sidebar.markdown("### Optimization priorities")
energy_weight = st.sidebar.slider("Energy priority", 0.5, 2.0, 1.4, 0.1)
latency_weight = st.sidebar.slider("Latency priority", 0.5, 2.0, 1.1, 0.1)
sla_weight = st.sidebar.slider("SLA priority", 0.5, 2.0, 1.2, 0.1)
focus_candidates = [algo for algo in MODERN_ALGOS if algo in algorithms] or algorithms
selected_focus = st.sidebar.selectbox("Highlight optimized algorithm", focus_candidates)
playback_algorithm = st.sidebar.selectbox("Playback algorithm", algorithms, index=max(algorithms.index(selected_focus), 0))
playback_speed = st.sidebar.slider("Animation speed (seconds)", 0.02, 0.30, 0.10, 0.02)

filtered_workload = workload_df[
    workload_df["submit_date"].between(start_date, end_date)
    & workload_df["job_type"].isin(job_types)
    & workload_df["priority_level"].isin(priorities)
].copy()

if filtered_workload.empty:
    st.warning("The selected workload filters returned no jobs. Adjust the sidebar filters.")
    st.stop()

if metric_mode == "Projection (what-if)":
    summary_df, per_job_df, cluster_pressure = simulate_algorithm_metrics(
        workload=filtered_workload,
        results_lookup=results_lookup,
        algorithms=algorithms,
        scenario_name=scenario,
        host_count=host_count,
        host_cpu_capacity=host_cpu_capacity,
        host_memory_capacity=host_memory_capacity,
        energy_weight=energy_weight,
        latency_weight=latency_weight,
        sla_weight=sla_weight,
    )
else:
    summary_df = build_measured_summary(results_lookup, algorithms)
    per_job_df = pd.DataFrame()
    cluster_pressure = (
        (
            filtered_workload["cpu_requested"].sum() / max(host_count * host_cpu_capacity, 1)
            + filtered_workload["memory_requested_gb"].sum() / max(host_count * host_memory_capacity, 1)
        )
        / 2
    )

if summary_df.empty:
    st.warning("Choose at least one algorithm to compare.")
    st.stop()

if selected_focus not in summary_df["algorithm"].tolist():
    selected_focus = summary_df.iloc[0]["algorithm"]

focus_row = summary_df[summary_df["algorithm"] == selected_focus].iloc[0]
legacy_subset = summary_df[summary_df["algorithm"].isin(LEGACY_ALGOS)]
legacy_reference = legacy_subset.mean(numeric_only=True) if not legacy_subset.empty else summary_df.mean(numeric_only=True)
best_legacy = (
    legacy_subset.sort_values("compositeScore", ascending=False).iloc[0]
    if not legacy_subset.empty
    else summary_df.sort_values("compositeScore").iloc[0]
)

jobs_in_scope = len(filtered_workload)
peak_hour = int(filtered_workload.groupby("submit_hour").size().sort_values(ascending=False).index[0])
avg_wait = filtered_workload["queue_wait_minutes"].mean()
cpu_pressure = filtered_workload["cpu_ratio"].mean() * 100
mem_pressure = filtered_workload["memory_ratio"].mean() * 100
energy_advantage = (legacy_reference["energyConsumption"] - focus_row["energyConsumption"]) / legacy_reference["energyConsumption"] * 100
latency_advantage = (legacy_reference["averageResponseTime"] - focus_row["averageResponseTime"]) / legacy_reference["averageResponseTime"] * 100

st.markdown(
    f"""
    <div class="hero-card">
        <div class="eyebrow">Intelligent VM Optimizer</div>
        <div class="hero-title">Controls now drive the optimization story in real time</div>
        <div class="hero-copy">
            Every filter and tuning knob now changes the algorithm outputs using the selected slice of
            <strong>cloud_workload_dataset.csv</strong>, the host configuration, and your optimization priorities.
        </div>
        <div class="leader">
            <div class="section-copy">
                Under <strong>{scenario}</strong>, the filtered workload is pushing the cluster to
                <strong>{cluster_pressure:.2f}x pressure</strong>. <strong>{selected_focus}</strong> is currently delivering
                <strong>{energy_advantage:.1f}%</strong> lower energy use and
                <strong>{latency_advantage:.1f}%</strong> faster response than the legacy average.
            </div>
        </div>
    </div>
    """,
    unsafe_allow_html=True,
)
if metric_mode != "Projection (what-if)":
    st.info(
        "Measured mode is using `results/experiment_results.csv` aggregates. Workload filters still drive context visuals, not KPI recomputation."
    )

top_metrics = st.columns(4)
with top_metrics[0]:
    metric_card("Jobs in scope", f"{jobs_in_scope:,}", f"Peak submit hour: {peak_hour:02d}:00")
with top_metrics[1]:
    metric_card("CPU pressure", f"{cpu_pressure:.1f}%", f"Memory pressure: {mem_pressure:.1f}%")
with top_metrics[2]:
    metric_card("Cluster shape", f"{host_count} hosts", f"{host_cpu_capacity} CPU / {host_memory_capacity} GB each")
with top_metrics[3]:
    metric_card("Top score", f"{summary_df.iloc[0]['compositeScore']:.1f}", f"Leader: {summary_df.iloc[0]['algorithm']}")

overview_tab, algorithms_tab, workload_tab, playback_tab = st.tabs(
    ["Overview", "Algorithm Advantage", "Workload Intelligence", "Animated Walkthrough"]
)

with overview_tab:
    section_heading(
        "Interactive overview",
        "These views now react to workload filters, host sizing, and optimization priorities instead of sitting on a static result row.",
    )
    left, right = st.columns([1.15, 1])

    with left:
        hourly = (
            filtered_workload.groupby("submit_hour")
            .agg(job_count=("job_id", "count"), cpu_requested=("cpu_requested", "sum"))
            .reset_index()
        )
        fig, ax1 = plt.subplots(figsize=(9, 4.5))
        ax2 = ax1.twinx()
        sns.barplot(data=hourly, x="submit_hour", y="job_count", color="#1b6b63", ax=ax1)
        sns.lineplot(data=hourly, x="submit_hour", y="cpu_requested", color="#e07a3f", marker="o", linewidth=2.5, ax=ax2)
        ax1.set_title("Hourly demand profile")
        ax1.set_xlabel("Hour")
        ax1.set_ylabel("Jobs")
        ax2.set_ylabel("Requested CPUs")
        st.pyplot(fig, use_container_width=True)

    with right:
        fig, ax = plt.subplots(figsize=(7.8, 4.5))
        sns.barplot(
            data=summary_df,
            y="algorithm",
            x="compositeScore",
            palette=["#163f3c" if algo == selected_focus else "#d9b08c" for algo in summary_df["algorithm"]],
            ax=ax,
        )
        ax.set_title("Composite ranking after control adjustments")
        ax.set_xlabel("Composite score")
        ax.set_ylabel("")
        st.pyplot(fig, use_container_width=True)

    lower_left, lower_right = st.columns(2)
    with lower_left:
        if per_job_df.empty:
            st.caption("Per-job energy view is available in Projection mode.")
        else:
            trend_df = (
                per_job_df.groupby(["algorithm", "job_type"])["energyConsumption"]
                .mean()
                .reset_index()
            )
            fig, ax = plt.subplots(figsize=(7.8, 4.4))
            sns.barplot(data=trend_df, x="job_type", y="energyConsumption", hue="algorithm", ax=ax)
            ax.set_title("Energy behavior by job type")
            ax.set_xlabel("")
            ax.set_ylabel("Average energy")
            ax.tick_params(axis="x", rotation=18)
            st.pyplot(fig, use_container_width=True)

    with lower_right:
        fig, ax = plt.subplots(figsize=(7.8, 4.4))
        priority_df = filtered_workload["priority_level"].value_counts()
        ax.pie(
            priority_df.values,
            labels=priority_df.index,
            autopct="%1.0f%%",
            startangle=90,
            colors=["#163f3c", "#e07a3f", "#f0d6b8"],
            wedgeprops={"linewidth": 1, "edgecolor": "white"},
        )
        ax.set_title("Priority distribution in current slice")
        st.pyplot(fig, use_container_width=True)

with algorithms_tab:
    section_heading(
        "Why the new algorithms outperform the others",
        "This section compares the selected modern algorithm against the legacy schedulers using the current controls and filtered workload.",
    )

    best_energy = summary_df.sort_values("energyConsumption").iloc[0]
    best_latency = summary_df.sort_values("averageResponseTime").iloc[0]
    best_sla = summary_df.sort_values("slaCompliance", ascending=False).iloc[0]
    st.markdown(
        f"""
        <div class="glass-card">
            <div class="section-copy">
                <strong>Executive recommendation:</strong> choose <strong>{summary_df.iloc[0]['algorithm']}</strong> for balanced outcomes.
                Cost leader: <strong>{best_energy['algorithm']}</strong>, latency leader: <strong>{best_latency['algorithm']}</strong>,
                SLA leader: <strong>{best_sla['algorithm']}</strong>.
            </div>
        </div>
        """,
        unsafe_allow_html=True,
    )

    delta_cols = st.columns(4)
    with delta_cols[0]:
        metric_card("Energy reduction", f"{energy_advantage:.1f}%", "vs legacy average")
    with delta_cols[1]:
        metric_card("Latency improvement", f"{latency_advantage:.1f}%", "vs legacy average")
    with delta_cols[2]:
        metric_card("SLA uplift", f"{(focus_row['slaCompliance'] - legacy_reference['slaCompliance']) * 100:.1f} pts", "vs legacy average")
    with delta_cols[3]:
        metric_card("Utilization lift", f"{(focus_row['utilization'] - legacy_reference['utilization']) * 100:.1f} pts", "vs legacy average")

    top_left, top_right = st.columns([1.05, 1])
    with top_left:
        heatmap_df = summary_df.set_index("algorithm")[
            ["energy_score", "latency_score", "sla_score", "util_score", "eff_score"]
        ].rename(
            columns={
                "energy_score": "Energy",
                "latency_score": "Latency",
                "sla_score": "SLA",
                "util_score": "Utilization",
                "eff_score": "Efficiency",
            }
        )
        fig, ax = plt.subplots(figsize=(8.2, 4.7))
        sns.heatmap(heatmap_df, annot=True, fmt=".2f", cmap="YlGnBu", linewidths=0.4, cbar=False, ax=ax)
        ax.set_title("Normalized strengths")
        ax.set_xlabel("")
        ax.set_ylabel("")
        st.pyplot(fig, use_container_width=True)

    with top_right:
        compare_df = summary_df[
            ["algorithm", "energyConsumption", "averageResponseTime", "slaCompliance"]
        ].melt("algorithm", var_name="metric", value_name="value")
        fig, ax = plt.subplots(figsize=(8, 4.7))
        sns.barplot(data=compare_df, x="metric", y="value", hue="algorithm", ax=ax)
        ax.set_title("Head-to-head comparison")
        ax.set_xlabel("")
        ax.set_ylabel("Observed value")
        ax.tick_params(axis="x", rotation=15)
        st.pyplot(fig, use_container_width=True)

    radar_left, radar_right = st.columns(2)
    with radar_left:
        fig, ax = plt.subplots(figsize=(7.8, 4.4))
        radar_df = radar_frame(summary_df)
        sns.lineplot(data=radar_df, x="dimension", y="score", hue="algorithm", marker="o", ax=ax)
        ax.set_title("Multi-objective profile")
        ax.set_xlabel("")
        ax.set_ylabel("Normalized score")
        ax.tick_params(axis="x", rotation=20)
        st.pyplot(fig, use_container_width=True)
    with radar_right:
        pareto = summary_df[["algorithm", "energyConsumption", "slaCompliance", "averageResponseTime"]].copy()
        fig, ax = plt.subplots(figsize=(7.8, 4.4))
        sns.scatterplot(
            data=pareto,
            x="energyConsumption",
            y="slaCompliance",
            hue="algorithm",
            size="averageResponseTime",
            sizes=(80, 280),
            ax=ax,
        )
        ax.set_title("Pareto view: energy vs SLA (bubble=response)")
        st.pyplot(fig, use_container_width=True)

    lane_left, lane_right = st.columns(2)
    with lane_left:
        if per_job_df.empty:
            st.caption("Per-job spread plots are available in Projection mode.")
        else:
            energy_box = per_job_df[per_job_df["algorithm"].isin(algorithms)][["algorithm", "energyConsumption"]]
            fig, ax = plt.subplots(figsize=(7.8, 4.4))
            sns.boxplot(data=energy_box, x="algorithm", y="energyConsumption", ax=ax)
            ax.set_title("Per-job energy spread")
            ax.tick_params(axis="x", rotation=18)
            st.pyplot(fig, use_container_width=True)

    with lane_right:
        if per_job_df.empty:
            st.caption("Latency-by-priority is available in Projection mode.")
        else:
            response_trend = (
                per_job_df.groupby(["algorithm", "priority_level"])["averageResponseTime"]
                .mean()
                .reset_index()
            )
            fig, ax = plt.subplots(figsize=(7.8, 4.4))
            sns.barplot(data=response_trend, x="priority_level", y="averageResponseTime", hue="algorithm", ax=ax)
            ax.set_title("Latency by priority")
            ax.set_xlabel("")
            ax.set_ylabel("Average response time")
            st.pyplot(fig, use_container_width=True)

    st.dataframe(
        summary_df[
            [
                "rank",
                "algorithm",
                "compositeScore",
                "energyConsumption",
                "averageResponseTime",
                "slaCompliance",
                "utilization",
                "energyEfficiency",
            ]
        ]
        .rename(
            columns={
                "rank": "Rank",
                "algorithm": "Algorithm",
                "compositeScore": "Composite Score",
                "energyConsumption": "Energy Consumption",
                "averageResponseTime": "Avg Response Time",
                "slaCompliance": "SLA Compliance",
                "utilization": "Utilization",
                "energyEfficiency": "Energy Efficiency",
            }
        )
        .style.format(
            {
                "Composite Score": "{:.2f}",
                "Energy Consumption": "{:.2f}",
                "Avg Response Time": "{:.1f}",
                "SLA Compliance": "{:.2%}",
                "Utilization": "{:.2%}",
                "Energy Efficiency": "{:.2%}",
            }
        ),
        use_container_width=True,
    )
    st.caption(
        f"{selected_focus} is beating the strongest legacy scheduler, {best_legacy['algorithm']}, by "
        f"{((best_legacy['energyConsumption'] - focus_row['energyConsumption']) / best_legacy['energyConsumption']) * 100:.1f}% on energy "
        f"and {((best_legacy['averageResponseTime'] - focus_row['averageResponseTime']) / best_legacy['averageResponseTime']) * 100:.1f}% on response time."
    )

with workload_tab:
    section_heading(
        "Workload intelligence from the selected dataset slice",
        "These views show how the current filters reshape the pressure pattern that each algorithm has to manage.",
    )
    wl_left, wl_right = st.columns(2)

    with wl_left:
        heatmap_source = (
            filtered_workload.groupby(["weekday", "submit_hour"])["cpu_requested"]
            .sum()
            .reset_index()
            .pivot(index="weekday", columns="submit_hour", values="cpu_requested")
            .reindex(["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"])
            .fillna(0)
        )
        fig, ax = plt.subplots(figsize=(8.5, 4.6))
        sns.heatmap(heatmap_source, cmap="mako", linewidths=0.2, ax=ax)
        ax.set_title("Requested CPU heatmap by weekday/hour")
        ax.set_xlabel("Hour")
        ax.set_ylabel("")
        st.pyplot(fig, use_container_width=True)

    with wl_right:
        sample = filtered_workload.sample(min(320, len(filtered_workload)), random_state=42)
        fig, ax = plt.subplots(figsize=(8.5, 4.6))
        sns.scatterplot(
            data=sample,
            x="cpu_requested",
            y="duration",
            hue="job_type",
            size="memory_requested_gb",
            sizes=(40, 210),
            alpha=0.72,
            palette="Dark2",
            ax=ax,
        )
        ax.set_title("Request size vs execution time")
        ax.set_xlabel("Requested CPUs")
        ax.set_ylabel("Execution time (s)")
        st.pyplot(fig, use_container_width=True)

    bottom_left, bottom_right = st.columns([1, 1.05])
    with bottom_left:
        fig, ax = plt.subplots(figsize=(7.8, 4.4))
        sns.histplot(filtered_workload["queue_wait_minutes"], bins=22, color="#e07a3f", kde=True, ax=ax)
        ax.set_title("Queue wait distribution")
        ax.set_xlabel("Queue wait (minutes)")
        ax.set_ylabel("Jobs")
        st.pyplot(fig, use_container_width=True)

    with bottom_right:
        top_jobs = (
            filtered_workload.sort_values(["cpu_requested", "memory_requested_mb"], ascending=False)
            .head(10)[["job_id", "job_type", "priority_level", "cpu_requested", "memory_requested_gb", "duration", "queue_wait_seconds"]]
            .rename(
                columns={
                    "job_id": "Job",
                    "job_type": "Type",
                    "priority_level": "Priority",
                    "cpu_requested": "Requested CPUs",
                    "memory_requested_gb": "Requested Memory (GB)",
                    "duration": "Execution Time (s)",
                    "queue_wait_seconds": "Queue Wait (s)",
                }
            )
        )
        st.dataframe(top_jobs.style.format({"Requested Memory (GB)": "{:.1f}"}), use_container_width=True, height=360)

with playback_tab:
    section_heading(
        "Animated algorithm walkthrough",
        "This animation now shows how a selected algorithm scores hosts and places real jobs from the filtered workload.",
    )
    st.markdown(
        f"""
        <div class="glass-card">
            <div class="section-copy">
                <strong>{playback_algorithm}</strong> uses the strategy <strong>{ALGO_PROFILES[playback_algorithm]['label']}</strong>.
                Press the button to step through live host scoring and placement decisions.
            </div>
        </div>
        """,
        unsafe_allow_html=True,
    )
    play = st.button("Run scheduling animation", use_container_width=True)
    if play:
        render_algorithm_animation(
            workload=filtered_workload,
            algorithm=playback_algorithm,
            host_count=host_count,
            host_cpu_capacity=host_cpu_capacity,
            host_memory_capacity=host_memory_capacity,
            frame_delay=playback_speed,
        )
