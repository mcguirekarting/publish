# Inventory Reconciliation Microservice

This microservice reconciles inventory levels between Manhattan Active Omni (MAO) and Manhattan Active Warehouse Management (MAWM) systems.

## Overview

The service monitors inventory levels in both systems and, when discrepancies are found, updates the non-authoritative system to match the authoritative system according to configurable rules. This ensures inventory accuracy across your distributed systems.

### Key Features

- **Real-time Inventory Comparison**: Query both MAO and MAWM to compare inventory levels
- **Configurable Reconciliation Rules**: Set thresholds and identify which system is authoritative for specific items/locations
- **Scheduled Reconciliation**: Automatically perform reconciliation at configurable intervals
- **Circuit Breaking**: Prevent cascading failures using Resilience4j
- **Metrics & Monitoring**: Integrated with Prometheus and Actuator for operational visibility
- **Docker & Airflow Support**: Containerization and workflow orchestration ready
- **RESTful API**: API endpoints for manual reconciliation and configuration

## Architecture

```
┌─────────────────────────────────┐
│                                 │
│  Inventory Reconciliation       │
│  Microservice                   │
│                                 │
└─────────┬───────────┬───────────┘
          │           │
          ▼           ▼
┌─────────────┐ ┌─────────────┐
│             │ │             │
│  Manhattan  │ │  Manhattan  │
│  Active     │ │  Active WM  │
│  Omni       │ │             │
│             │ │             │
└─────────────┘ └─────────────┘
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- PostgreSQL database
- Docker and Docker Compose (optional)
- Apache Airflow (optional)

### Configuration

The application is configured using Spring Boot properties. Key configuration parameters:

- `manhattan.mao.url`: URL for the MAO API
- `manhattan.mawm.url`: URL for the MAWM API
- `manhattan.mao.auth.token`: Authentication token for MAO
- `manhattan.mawm.auth.token`: Authentication token for MAWM
- `reconciliation.default-threshold`: Minimum quantity difference to trigger reconciliation
- `reconciliation.default-authoritative-system`: Default system of record (MAO or MAWM)
- `reconciliation.items-to-reconcile`: List of item IDs to reconcile
- `reconciliation.locations-to-reconcile`: List of location IDs to reconcile

See `application.yml`, `application-dev.yml`, and `application-prod.yml` for complete configuration options.

### Building

```bash
./mvnw clean package
```

### Running Locally

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Running with Docker

```bash
# Build the Docker image
docker build -t inventory-reconciliation .

# Run with Docker Compose
docker-compose up -d
```

### Docker Environment Variables

Set these environment variables when running in Docker:

- `SPRING_PROFILES_ACTIVE`: Set to `dev` or `prod`
- `MANHATTAN_MAO_URL`: MAO API URL
- `MANHATTAN_MAWM_URL`: MAWM API URL
- `MANHATTAN_MAO_AUTH_TOKEN`: MAO authentication token
- `MANHATTAN_MAWM_AUTH_TOKEN`: MAWM authentication token
- `MANHATTAN_MAO_ORGANIZATION`: MAO organization ID
- `MANHATTAN_MAWM_ORGANIZATION`: MAWM organization ID
- `SPRING_DATASOURCE_URL`: JDBC URL for your database
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password

### Airflow Integration

To use with Apache Airflow instead of the built-in scheduler:

1. Copy the `inventory_reconciliation_dag.py` file to your Airflow DAGs folder
2. Configure the Airflow connection using the provided script
3. Set `reconciliation.schedule.enabled=false` in your application properties

## API Endpoints

### Inventory Queries

- `GET /api/inventory/mao/{itemId}/{locationId}`: Get MAO inventory
- `GET /api/inventory/mawm/{itemId}/{locationId}`: Get MAWM inventory
- `GET /api/inventory/compare/{itemId}/{locationId}`: Compare inventory from both systems

### Reconciliation

- `POST /api/inventory/reconcile`: Reconcile inventory for a specific item/location
  - Body: `{ "itemId": "...", "locationId": "...", "forceReconciliation": false }`
- `GET /api/inventory/reconcile/history/{itemId}/{locationId}`: Get reconciliation history

### System of Record Configuration

- `GET /api/inventory/system-of-record/{itemId}/{locationId}`: Get system of record configuration
- `POST /api/inventory/system-of-record`: Set system of record configuration
  - Body: `{ "itemId": "...", "locationId": "...", "authoritative": "MAO", "threshold": 1.0 }`

## Best Practices for Implementation

1. **Incremental Rollout**: Start with a few critical SKUs before expanding to your entire inventory
2. **Monitoring**: Set up alerts for failed reconciliations and significant inventory discrepancies
3. **Performance Tuning**: Schedule bulk reconciliations during off-peak hours
4. **Security**: Ensure API tokens have the minimum required permissions
5. **Audit Trail**: Maintain a detailed log of reconciliation actions for auditing purposes

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin feature/your-feature`
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.