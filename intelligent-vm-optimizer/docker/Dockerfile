FROM python:3.11-slim

RUN apt-get update && apt-get install -y openjdk-17-jdk maven && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY . /app

RUN pip install --no-cache-dir -r analysis/python_analysis/requirements.txt
RUN mvn -q -DskipTests package

CMD ["bash", "-lc", "mvn -q exec:java && python analysis/python_analysis/analyze_results.py && streamlit run visualization/dashboard/app.py --server.address 0.0.0.0 --server.port 8501"]
