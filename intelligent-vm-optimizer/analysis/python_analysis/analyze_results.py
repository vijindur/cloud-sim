import json
from pathlib import Path
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from scipy import stats

RESULTS = Path("results/experiment_results.csv")
OUT = Path("results/plots")
OUT.mkdir(parents=True, exist_ok=True)


def anova(df: pd.DataFrame, metric: str):
    groups = [g[metric].values for _, g in df.groupby("algorithm")]
    return stats.f_oneway(*groups)


def plot_metric(df: pd.DataFrame, metric: str, title: str, filename: str):
    plt.figure(figsize=(10, 6))
    sns.boxplot(data=df, x="algorithm", y=metric)
    plt.xticks(rotation=30)
    plt.title(title)
    plt.tight_layout()
    plt.savefig(OUT / filename)
    plt.close()


def plot_heatmap(df: pd.DataFrame):
    grouped = df.groupby(["algorithm", "workload"])["utilization"].mean().unstack(fill_value=0)
    plt.figure(figsize=(10, 5))
    sns.heatmap(grouped, annot=True, cmap="viridis", fmt=".2f")
    plt.title("Resource Utilization Heatmap")
    plt.tight_layout()
    plt.savefig(OUT / "utilization_heatmap.png")
    plt.close()


def plot_pareto(df: pd.DataFrame):
    pareto = df.groupby("algorithm", as_index=False).agg(
        {"energyConsumption": "mean", "slaCompliance": "mean", "averageResponseTime": "mean"}
    )
    plt.figure(figsize=(8, 6))
    sns.scatterplot(
        data=pareto,
        x="energyConsumption",
        y="slaCompliance",
        hue="algorithm",
        size="averageResponseTime",
        sizes=(80, 220),
    )
    plt.title("Pareto Frontier: Energy vs SLA vs Response")
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
    if "migrationCount" in df.columns:
        plot_metric(df, "migrationCount", "Migration Count Comparison", "migration_boxplot.png")
    if "avgQueueDelay" in df.columns:
        plot_metric(df, "avgQueueDelay", "Queue Delay Comparison", "queue_delay_boxplot.png")
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
            runs=("runSeed", "count"),
            energy_mean=("energyConsumption", "mean"),
            energy_std=("energyConsumption", "std"),
            response_mean=("averageResponseTime", "mean"),
            response_std=("averageResponseTime", "std"),
            sla_mean=("slaCompliance", "mean"),
            sla_std=("slaCompliance", "std"),
            utilization_mean=("utilization", "mean"),
            utilization_std=("utilization", "std"),
            migration_mean=("migrationCount", "mean") if "migrationCount" in df.columns else ("energyConsumption", "count"),
            queue_delay_mean=("avgQueueDelay", "mean") if "avgQueueDelay" in df.columns else ("averageResponseTime", "mean"),
        )
        .reset_index()
    )
    summary.to_csv(OUT / "summary_stats.csv", index=False)
    (OUT / "anova_report.json").write_text(json.dumps(report, indent=2))
    print("Analysis completed")


if __name__ == "__main__":
    main()
