from pathlib import Path
import pandas as pd
import streamlit as st
import seaborn as sns
import matplotlib.pyplot as plt

st.set_page_config(page_title="Intelligent VM Optimizer", layout="wide")
st.title("Intelligent Resource Optimization for Virtual Machines")

result_path = Path("results/experiment_results.csv")
if not result_path.exists():
    st.warning("No results found. Run the simulation first.")
    st.stop()

df = pd.read_csv(result_path)
algorithms = st.multiselect("Algorithms", df["algorithm"].unique(), default=list(df["algorithm"].unique()))
selected = df[df["algorithm"].isin(algorithms)]

col1, col2 = st.columns(2)

with col1:
    st.subheader("Average Utilization")
    util = selected.groupby("algorithm", as_index=False)["utilization"].mean()
    st.bar_chart(util, x="algorithm", y="utilization")

with col2:
    st.subheader("Energy Consumption")
    energy = selected.groupby("algorithm", as_index=False)["energyConsumption"].mean()
    st.bar_chart(energy, x="algorithm", y="energyConsumption")

st.subheader("Performance Ranking")
rank = selected.groupby("algorithm", as_index=False).agg({
    "utilization": "mean",
    "slaCompliance": "mean",
    "energyEfficiency": "mean",
    "averageResponseTime": "mean"
})
rank["score"] = 0.4 * rank["utilization"] + 0.35 * rank["slaCompliance"] + 0.25 * rank["energyEfficiency"]
rank = rank.sort_values("score", ascending=False)
st.dataframe(rank)

st.subheader("SLA Compliance Distribution")
fig, ax = plt.subplots(figsize=(10, 4))
sns.violinplot(data=selected, x="algorithm", y="slaCompliance", ax=ax)
plt.xticks(rotation=30)
st.pyplot(fig)
