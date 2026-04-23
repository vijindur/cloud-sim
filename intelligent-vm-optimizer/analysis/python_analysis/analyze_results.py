import csv
import json
import math
from pathlib import Path
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from scipy import stats

RESULTS = Path("results/experiment_results.csv")
OUT = Path("results/plots")
OUT.mkdir(parents=True, exist_ok=True)

METRICS = [
    ("utilization", "Utilization Comparison", "utilization.svg"),
    ("slaCompliance", "SLA Compliance Comparison", "sla.svg"),
    ("energyConsumption", "Energy Consumption Comparison", "energy.svg"),
    ("averageResponseTime", "Response Time Comparison", "response_time.svg"),
]


def load_rows():
    if not RESULTS.exists():
        raise FileNotFoundError(f"Expected results file not found: {RESULTS}")
    with RESULTS.open() as f:
        rows = list(csv.DictReader(f))
    if not rows:
        raise ValueError(f"No experiment rows found in {RESULTS}")
    return rows


def grouped_metric(rows, metric):
    groups = {}
    for row in rows:
        groups.setdefault(row["algorithm"], []).append(float(row[metric]))
    return groups

def plot_heatmap(df: pd.DataFrame):
    grouped = df.groupby(["algorithm", "workload"])["utilization"].mean().unstack(fill_value=0)
    plt.figure(figsize=(10, 5))
    sns.heatmap(grouped, annot=True, cmap="viridis", fmt=".2f")
    plt.title("Resource Utilization Heatmap")
    plt.tight_layout()
    plt.savefig(OUT / "utilization_heatmap.png")
    plt.close()


def plot_pareto(df: pd.DataFrame):
    pareto = df.groupby("algorithm", as_index=False).agg({"energyConsumption": "mean", "slaCompliance": "mean"})
    plt.figure(figsize=(8, 6))
    sns.scatterplot(data=pareto, x="energyConsumption", y="slaCompliance", hue="algorithm", s=120)
    plt.title("Pareto Frontier: Energy vs SLA")
    plt.tight_layout()
    plt.savefig(OUT / "pareto_frontier.png")
    plt.close()


def plot_convergence(df: pd.DataFrame):
    subset = df[df["algorithm"].isin(["PSO_STANDARD", "PSO_MODIFIED"])].copy()
    if subset.empty or "convergenceStart" not in subset.columns or "convergenceEnd" not in subset.columns:
        return

    points = []
    for _, row in subset.iterrows():
        points.append({"algorithm": row["algorithm"], "iteration": 0, "fitness": row["convergenceStart"]})
        points.append({"algorithm": row["algorithm"], "iteration": 50, "fitness": row["convergenceEnd"]})
    cdf = pd.DataFrame(points)
    plt.figure(figsize=(8, 6))
    sns.lineplot(data=cdf, x="iteration", y="fitness", hue="algorithm", marker="o")
    plt.title("Convergence Trend (start/end)")
    plt.tight_layout()
    plt.savefig(OUT / "convergence.png")
    plt.close()


def main():
    df = pd.read_csv(RESULTS)
    plot_metric(df, "utilization", "Utilization Comparison", "utilization_boxplot.png")
    plot_metric(df, "slaCompliance", "SLA Compliance Comparison", "sla_boxplot.png")
    plot_metric(df, "energyConsumption", "Energy Consumption Comparison", "energy_boxplot.png")
    plot_metric(df, "averageResponseTime", "Response Time Comparison", "response_time_boxplot.png")
    plot_heatmap(df)
    plot_pareto(df)
    plot_convergence(df)

    report = {
        "utilization_anova": anova(df, "utilization")._asdict(),
        "sla_anova": anova(df, "slaCompliance")._asdict(),
        "energy_anova": anova(df, "energyConsumption")._asdict(),
        "response_time_anova": anova(df, "averageResponseTime")._asdict(),
    }
    summary = (
        df.groupby(["algorithm", "workload"]).agg(
            energy_mean=("energyConsumption", "mean"),
            energy_std=("energyConsumption", "std"),
            response_mean=("averageResponseTime", "mean"),
            response_std=("averageResponseTime", "std"),
        )
        .reset_index()
    )
    summary.to_csv(OUT / "summary_stats.csv", index=False)
    (OUT / "anova_report.json").write_text(json.dumps(report, indent=2))
    print("Analysis completed")


if __name__ == "__main__":
    main()
