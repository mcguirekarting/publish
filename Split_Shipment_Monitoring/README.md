# Split Shipment Monitoring Microservice for OMS

This microservice monitors and analyzes split shipment rates in Manhattan Active Omni. It provides a dashboard with detailed metrics, alerts when split rates exceed thresholds, and generates exportable reports.

## Features

- **Data Collection**: Automatically collects split shipment data from Manhattan Active Omni APIs
- **Analysis**: Analyzes trends and calculates key metrics for split shipments
- **Alerting**: Sends alerts when split rates exceed configurable thresholds
- **Reporting**: Generates CSV and PowerPoint reports for different time periods
- **Dashboard**: Web-based dashboard with filtering capabilities
- **API**: RESTful API to access all data and reports

## Architecture

The microservice is built using the following components:

- **Airflow**: Orchestrates data collection, analysis, and report generation
- **Flask**: Powers the API service
- **Redis**: Caches data for fast dashboard access
- **AWS S3**: Stores collected data and generated reports
- **Nginx**: Serves the dashboard frontend

## Getting Started

### Prerequisites

- Docker and Docker Compose
- AWS account with S3 access
- Manhattan Active Omni instance with API access

### Installation

1. Clone this repository
2. Copy `.env.template` to `.env` and fill in your credentials
3. Start the services:

```bash
docker-compose up -d
```

4. Access the dashboard at http://localhost
5. Access the Airflow UI at http://localhost:8080

### Configuration

The microservice can be configured using environment variables in the `.env` file:

- **Manhattan Active Omni Configuration**: 
  - `MANH_BASE_URL`: The base URL of your Manhattan Active Omni instance
  - `MANH_CLIENT_ID`: API client ID
  - `MANH_CLIENT_SECRET`: API client secret

- **AWS Configuration**:
  - `AWS_ACCESS_KEY_ID`: AWS access key
  - `AWS_SECRET_ACCESS_KEY`: AWS secret key
  - `AWS_REGION`: AWS region
  - `S3_DATA_BUCKET`: S3 bucket name for data storage

- **Organization Configuration**:
  - `ORGANIZATION_LIST`: JSON array of organization IDs to monitor

- **Alert Configuration**:
  - `SLACK_WEBHOOK_URL`: Slack webhook URL for alerts
  - `ALERT_EMAIL_RECIPIENTS`: JSON array of email addresses for alerts

## Dashboard

The dashboard provides the following views:

- **Overview**: Summary of split shipment rates across all organizations
- **Organization View**: Detailed view for a specific organization
- **Location Analysis**: Split rates by location (node)
- **SKU Analysis**: Split rates by SKU
- **Alerts**: Active alerts and historical alert data
- **Reports**: Access to all generated reports

### Filtering

The dashboard supports filtering by:

- Date range
- Organization
- Location (node)
- SKU
- CO status

## API Documentation

The API service provides RESTful endpoints for accessing split shipment data:

- **GET /api/organizations**: List all available organizations
- **GET /api/historical/{organization}**: Get historical data for an organization
- **GET /api/analysis/{organization}**: Get latest analysis for an organization
- **GET /api/alerts**: Get all active alerts
- **GET /api/reports**: Get available reports
- **GET /api/reports/download/{report_key}**: Download a specific report
- **GET /api/dashboard/{organization}**: Get all dashboard data for an organization
- **POST /api/search**: Search for split shipment data based on complex criteria

## DAG Workflows

The microservice includes the following Airflow DAGs:

- **split_shipment_collector**: Collects data about split shipments
- **split_shipment_analyzer**: Analyzes trends and generates alerts
- **split_shipment_report_generator**: Generates CSV and PowerPoint reports
- **api_service**: Updates the API cache with the latest data

## Troubleshooting

### Common Issues

1. **API Authentication Failures**: Check your Manhattan API credentials
2. **Missing Data**: Verify that the collector DAG is running successfully
3. **S3 Access Errors**: Check your AWS credentials and bucket permissions
4. **Alert Notification Failures**: Verify your Slack webhook URL and email configuration

### Logs

- Airflow logs: `./logs/`
- API service logs: Docker logs for the `split-shipment-api` container
- Dashboard logs: Docker logs for the `dashboard-frontend` container

## License

This project is licensed under the MIT License - see the LICENSE file for details.