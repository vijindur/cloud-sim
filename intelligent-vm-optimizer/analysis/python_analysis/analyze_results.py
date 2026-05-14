import json
from pathlib import Path
import numpy as np
import pandas as pd
import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import seaborn as sns
from scipy import stats

RESULTS = Path("results/experiment_results.csv")
OUT = Path("results/plots")
OUT.mkdir(parents=True, exist_ok=True)


def anova(df: pd.DataFrame, metric: str):
    groups = [g[metric].dropna().values for _, g in df.groupby("algorithm") if len(g[metric].dropna()) > 0]
    if len(groups) < 2:
        return {"statistic": None, "pvalue": None, "note": "ANOVA requires at least two algorithm groups."}
    result = stats.f_oneway(*groups)
    return result._asdict()


def confidence_interval(series: pd.Series, confidence: float = 0.95) -> tuple[float, float]:
    values = series.dropna().values
    if len(values) < 2:
        mean = float(np.mean(values)) if len(values) == 1 else 0.0
        return mean, mean
    mean = float(np.mean(values))
    sem = stats.sem(values)
    margin = sem * stats.t.ppf((1 + confidence) / 2.0, len(values) - 1)
    return mean - margin, mean + margin


def cohens_d(a: pd.Series, b: pd.Series) -> float:
    va = a.dropna().values
    vb = b.dropna().values
    if len(va) < 2 or len(vb) < 2:
        return 0.0
    pooled = np.sqrt((((len(va) - 1) * np.var(va, ddof=1)) + ((len(vb) - 1) * np.var(vb, ddof=1))) / (len(va) + len(vb) - 2))
    if pooled == 0:
        return 0.0
    return float((np.mean(va) - np.mean(vb)) / pooled)


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
    if df.empty:
        raise ValueError(f"No simulation rows found in {RESULTS}. Run the simulator before analysis.")
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
        "utilization_anova": anova(df, "utilization"),
        "sla_anova": anova(df, "slaCompliance"),
        "energy_anova": anova(df, "energyConsumption"),
        "response_time_anova": anova(df, "averageResponseTime"),
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
    ci_rows = []
    for (algorithm, workload), group in df.groupby(["algorithm", "workload"]):
        e_low, e_high = confidence_interval(group["energyConsumption"])
        r_low, r_high = confidence_interval(group["averageResponseTime"])
        sla_low, sla_high = confidence_interval(group["slaCompliance"])
        ci_rows.append(
            {
                "algorithm": algorithm,
                "workload": workload,
                "energy_ci95_low": e_low,
                "energy_ci95_high": e_high,
                "response_ci95_low": r_low,
                "response_ci95_high": r_high,
                "sla_ci95_low": sla_low,
                "sla_ci95_high": sla_high,
            }
        )
    ci_df = pd.DataFrame(ci_rows)

    ablation_rows = []
    for workload, wdf in df.groupby("workload"):
        hybrid = wdf[wdf["algorithm"] == "HYBRID"]
        pso_mod = wdf[wdf["algorithm"] == "PSO_MODIFIED"]
        best_fit = wdf[wdf["algorithm"] == "BEST_FIT"]
        if not hybrid.empty and not pso_mod.empty:
            ablation_rows.append(
                {
                    "workload": workload,
                    "comparison": "HYBRID_vs_PSO_MODIFIED",
                    "energy_delta_pct": (pso_mod["energyConsumption"].mean() - hybrid["energyConsumption"].mean()) / pso_mod["energyConsumption"].mean() * 100,
                    "latency_delta_pct": (pso_mod["averageResponseTime"].mean() - hybrid["averageResponseTime"].mean()) / pso_mod["averageResponseTime"].mean() * 100,
                    "sla_delta_pts": (hybrid["slaCompliance"].mean() - pso_mod["slaCompliance"].mean()) * 100,
                    "energy_effect_size_d": cohens_d(hybrid["energyConsumption"], pso_mod["energyConsumption"]),
                }
            )
        if not hybrid.empty and not best_fit.empty:
            ablation_rows.append(
                {
                    "workload": workload,
                    "comparison": "HYBRID_vs_BEST_FIT",
                    "energy_delta_pct": (best_fit["energyConsumption"].mean() - hybrid["energyConsumption"].mean()) / best_fit["energyConsumption"].mean() * 100,
                    "latency_delta_pct": (best_fit["averageResponseTime"].mean() - hybrid["averageResponseTime"].mean()) / best_fit["averageResponseTime"].mean() * 100,
                    "sla_delta_pts": (hybrid["slaCompliance"].mean() - best_fit["slaCompliance"].mean()) * 100,
                    "energy_effect_size_d": cohens_d(hybrid["energyConsumption"], best_fit["energyConsumption"]),
                }
            )
    ablation_df = pd.DataFrame(ablation_rows)

    summary.to_csv(OUT / "summary_stats.csv", index=False)
    ci_df.to_csv(OUT / "confidence_intervals.csv", index=False)
    ablation_df.to_csv(OUT / "ablation_study.csv", index=False)
    (OUT / "anova_report.json").write_text(json.dumps(report, indent=2))
    print("Analysis completed")


if __name__ == "__main__":
    main()
