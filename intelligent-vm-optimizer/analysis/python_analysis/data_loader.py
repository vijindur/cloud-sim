from pathlib import Path
import pandas as pd


def load_google_trace(path: str | Path) -> pd.DataFrame:
    df = pd.read_csv(path)
    return df.rename(columns={"cpu_usage": "cpu", "mem_usage": "memory"})


def load_alibaba_trace(path: str | Path) -> pd.DataFrame:
    df = pd.read_csv(path)
    return df.rename(columns={"cpu_avg": "cpu", "mem_avg": "memory"})


def to_vm_requests(df: pd.DataFrame) -> list[dict]:
    records = []
    for _, row in df.iterrows():
        records.append(
            {
                "vm_id": row.get("vm_id", row.get("instance_id", "unknown")),
                "cpu": float(row.get("cpu", 0.5)),
                "memory": float(row.get("memory", 0.5)),
                "duration": int(row.get("duration", 60)),
            }
        )
    return records
