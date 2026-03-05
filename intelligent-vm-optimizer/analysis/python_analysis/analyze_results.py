import json
from pathlib import Path
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


def main():
    df = pd.read_csv(RESULTS)
    plot_metric(df, "utilization", "Utilization Comparison", "utilization.png")
    plot_metric(df, "slaCompliance", "SLA Compliance Comparison", "sla.png")
    plot_metric(df, "energyConsumption", "Energy Consumption Comparison", "energy.png")
    plot_metric(df, "averageResponseTime", "Response Time Comparison", "response_time.png")

    report = {
        "utilization_anova": anova(df, "utilization")._asdict(),
        "sla_anova": anova(df, "slaCompliance")._asdict(),
        "energy_anova": anova(df, "energyConsumption")._asdict(),
        "response_time_anova": anova(df, "averageResponseTime")._asdict(),
    }
    (OUT / "anova_report.json").write_text(json.dumps(report, indent=2))
    print("Analysis completed")


if __name__ == "__main__":
    main()
