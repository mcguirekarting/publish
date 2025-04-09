# Use the official Airflow image as the base
FROM apache/airflow:2.5.1

# Copy requirements file
COPY requirements-airflow.txt /opt/airflow/requirements-airflow.txt

# Install dependencies as the airflow user
USER airflow
RUN pip install --upgrade pip && \
    pip install --no-cache-dir -r /opt/airflow/requirements-airflow.txt