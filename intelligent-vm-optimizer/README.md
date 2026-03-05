# Intelligent VM Optimizer

Research-grade simulation framework for intelligent VM scheduling using CloudSim Plus.

## Build and Run

```bash
mvn clean package
mvn exec:java
```

## Analysis

```bash
pip install -r analysis/python_analysis/requirements.txt
python analysis/python_analysis/analyze_results.py
```

## Dashboard

```bash
streamlit run visualization/dashboard/app.py
```

## Docker

```bash
docker-compose up --build
```
