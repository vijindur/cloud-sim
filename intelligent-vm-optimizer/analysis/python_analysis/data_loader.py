from pathlib import Path
import pandas as pd


def load_google_trace(path: str | Path) -> pd.DataFrame:
    df = pd.read_csv(path)
    return df.rename(columns={"cpu_usage": "cpu", "mem_usage": "memory"})


def load_alibaba_trace(path: str | Path) -> pd.DataFrame:
    df = pd.read_csv(path)
    return df.rename(columns={"cpu_avg": "cpu", "mem_avg": "memory"})


def load_cloud_workload_dataset(path: str | Path) -> pd.DataFrame:
    df = pd.read_csv(path)
    renamed = df.rename(
        columns={
            "Job_ID": "job_id",
            "Submit_Time": "submit_time",
            "Start_Time": "start_time",
            "End_Time": "end_time",
            "Requested_CPUs": "cpu_requested",
            "Used_CPUs": "cpu_used",
            "Requested_Memory(MB)": "memory_requested_mb",
            "Used_Memory(MB)": "memory_used_mb",
            "Execution_Time(Seconds)": "duration",
            "Queue_Wait_Time(Seconds)": "queue_wait_seconds",
            "User_ID": "user_id",
            "Job_Type": "job_type",
            "Priority_Level": "priority_level",
            "Node_Count": "node_count",
            "Interarrival_Time": "interarrival_time",
        }
    )

    for column in ("submit_time", "start_time", "end_time"):
        if column in renamed.columns:
            renamed[column] = pd.to_datetime(renamed[column], errors="coerce")

    return renamed


def to_vm_requests(df: pd.DataFrame) -> list[dict]:
    records = []
    for _, row in df.iterrows():
        records.append(
            {
                "vm_id": row.get("vm_id", row.get("instance_id", "unknown")),
                "cpu": float(row.get("cpu", row.get("cpu_requested", 0.5))),
                "memory": float(
                    row.get(
                        "memory",
                        row.get("memory_requested_mb", 512),
                    )
                ),
                "duration": int(row.get("duration", 60)),
            }
        )
    return records
