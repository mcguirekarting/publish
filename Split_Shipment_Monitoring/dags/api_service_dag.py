"""
Manhattan Active Omni - Split Shipment API Service DAG
-----------------------------------------------------
This DAG runs the API service that exposes the split shipment data and reports.
It updates the API cache with the latest data for fast dashboard response times.
"""

from datetime import datetime, timedelta
import json
import pandas as pd
import logging
from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.models import Variable
from airflow.providers.amazon.aws.hooks.s3 import S3Hook
from airflow.providers.redis.hooks.redis import RedisHook

# Set up logging
logger = logging.getLogger(__name__)

# Default arguments for the DAG
default_args = {
    'owner': 'airflow',
    'depends_on_past': False,
    'email_on_failure': True,
    'email_on_retry': False,
    'retries': 1,
    'retry_delay': timedelta(minutes=5),
    'start_date': datetime(2023, 1, 1),
}

# Create the DAG
dag = DAG(
    'split_shipment_api_service',
    default_args=default_args,
    description='Updates the API cache with the latest split shipment data',
    schedule_interval='*/15 * * * *',  # Every 15 minutes
    catchup=False,
    tags=['manhattan', 'split_shipment', 'monitoring', 'api'],
)

# Environment variables
ORG_LIST = Variable.get("organization_list", deserialize_json=True)
S3_BUCKET = Variable.get("s3_data_bucket")
S3_PREFIX = "split_shipment_data"
REPORTS_PREFIX = "split_shipment_reports"
REDIS_CACHE_KEY_PREFIX = "split_shipment"

def list_available_reports(**kwargs):
    """List all available reports in S3."""
    try:
        s3_hook = S3Hook()
        
        # Get a list of all reports
        report_types = ['daily', 'weekly', 'monthly']
        all_reports = []
        
        for report_type in report_types:
            for org in ORG_LIST:
                prefix = f"{REPORTS_PREFIX}/{report_type}/{org}/"
                report_keys = s3_hook.list_keys(
                    bucket_name=S3_BUCKET,
                    prefix=prefix
                )
                
                # Filter for metadata files which contain information about the reports
                metadata_keys = [key for key in report_keys if key.endswith('.metadata.json')]
                
                for metadata_key in metadata_keys:
                    try:
                        metadata_str = s3_hook.read_key(
                            key=metadata_key,
                            bucket_name=S3_BUCKET
                        )
                        metadata = json.loads(metadata_str)
                        all_reports.append(metadata)
                    except Exception as e:
                        logger.warning(f"Could not load metadata from {metadata_key}: {str(e)}")
        
        # Update the cache with the list of reports
        redis_hook = RedisHook(redis_conn_id='redis_default')
        redis_conn = redis_hook.get_conn()
        
        # Store the list of reports
        redis_conn.set(
            f"{REDIS_CACHE_KEY_PREFIX}:reports:all",
            json.dumps(all_reports),
            ex=86400  # Expire after 24 hours
        )
        
        # Create indexes for quick filtering
        report_by_org = {}
        report_by_type = {}
        report_by_date = {}
        
        for report in all_reports:
            org = report.get('organization')
            report_type = report.get('report_type')
            created_at = report.get('created_at')
            
            # Index by organization
            if org not in report_by_org:
                report_by_org[org] = []
            report_by_org[org].append(report)
            
            # Index by report type
            if report_type not in report_by_type:
                report_by_type[report_type] = []
            report_by_type[report_type].append(report)
            
            # Index by date (YYYY-MM-DD)
            if created_at:
                date = created_at.split('T')[0]
                if date not in report_by_date:
                    report_by_date[date] = []
                report_by_date[date].append(report)
        
        # Store the indexes
        for org, reports in report_by_org.items():
            redis_conn.set(
                f"{REDIS_CACHE_KEY_PREFIX}:reports:by_org:{org}",
                json.dumps(reports),
                ex=86400  # Expire after 24 hours
            )
        
        for report_type, reports in report_by_type.items():
            redis_conn.set(
                f"{REDIS_CACHE_KEY_PREFIX}:reports:by_type:{report_type}",
                json.dumps(reports),
                ex=86400  # Expire after 24 hours
            )
        
        for date, reports in report_by_date.items():
            redis_conn.set(
                f"{REDIS_CACHE_KEY_PREFIX}:reports:by_date:{date}",
                json.dumps(reports),
                ex=86400  # Expire after 24 hours
            )
        
        return {
            'total_reports': len(all_reports),
            'organizations': list(report_by_org.keys()),
            'report_types': list(report_by_type.keys()),
            'dates': list(report_by_date.keys())
        }
    
    except Exception as e:
        logger.error(f"Failed to list available reports: {str(e)}")
        raise

def update_dashboard_data(**kwargs):
    """Update the dashboard data in the cache."""
    try:
        s3_hook = S3Hook()
        redis_hook = RedisHook(redis_conn_id='redis_default')
        redis_conn = redis_hook.get_conn()
        
        # For each organization, load the latest analysis and summary data
        for org in ORG_LIST:
            # Get today's date
            today = datetime.now().strftime('%Y-%m-%d')
            yesterday = (datetime.now() - timedelta(days=1)).strftime('%Y-%m-%d')
            
            # Try to load today's analysis, then yesterday's if not available
            analysis_data = None
            for date in [today, yesterday]:
                analysis_key = f"{S3_PREFIX}/analysis/{org}/{date}_analysis.json"
                try:
                    analysis_str = s3_hook.read_key(
                        key=analysis_key,
                        bucket_name=S3_BUCKET
                    )
                    analysis_data = json.loads(analysis_str)
                    break
                except Exception as e:
                    logger.warning(f"Could not load analysis data for {org} on {date}: {str(e)}")
            
            # Store the analysis data in cache
            if analysis_data:
                redis_conn.set(
                    f"{REDIS_CACHE_KEY_PREFIX}:analysis:{org}",
                    json.dumps(analysis_data),
                    ex=86400  # Expire after 24 hours
                )
            
            # Load historical data
            historical_key = f"{S3_PREFIX}/historical/{org}_split_rates.json"
            try:
                historical_str = s3_hook.read_key(
                    key=historical_key,
                    bucket_name=S3_BUCKET
                )
                historical_data = json.loads(historical_str)
                
                # Store the historical data in cache
                redis_conn.set(
                    f"{REDIS_CACHE_KEY_PREFIX}:historical:{org}",
                    json.dumps(historical_data),
                    ex=86400  # Expire after 24 hours
                )
                
                # Process historical data for time-based views
                df = pd.DataFrame(historical_data)
                
                if not df.empty:
                    # Add date field in datetime format
                    df['date'] = pd.to_datetime(df['date'])
                    
                    # Last 7 days
                    last_7_days = df[df['date'] >= (datetime.now() - timedelta(days=7))]
                    redis_conn.set(
                        f"{REDIS_CACHE_KEY_PREFIX}:historical:7days:{org}",
                        json.dumps(last_7_days.to_dict('records')),
                        ex=86400  # Expire after 24 hours
                    )
                    
                    # Last 30 days
                    last_30_days = df[df['date'] >= (datetime.now() - timedelta(days=30))]
                    redis_conn.set(
                        f"{REDIS_CACHE_KEY_PREFIX}:historical:30days:{org}",
                        json.dumps(last_30_days.to_dict('records')),
                        ex=86400  # Expire after 24 hours
                    )
                    
                    # Last 90 days
                    last_90_days = df[df['date'] >= (datetime.now() - timedelta(days=90))]
                    redis_conn.set(
                        f"{REDIS_CACHE_KEY_PREFIX}:historical:90days:{org}",
                        json.dumps(last_90_days.to_dict('records')),
                        ex=86400  # Expire after 24 hours
                    )
            except Exception as e:
                logger.warning(f"Could not load historical data for {org}: {str(e)}")
        
        # Update the list of available data
        redis_conn.set(
            f"{REDIS_CACHE_KEY_PREFIX}:organizations",
            json.dumps(ORG_LIST),
            ex=86400  # Expire after 24 hours
        )
        
        # Update last cache refresh time
        redis_conn.set(
            f"{REDIS_CACHE_KEY_PREFIX}:last_updated",
            datetime.now().isoformat(),
            ex=86400  # Expire after 24 hours
        )
        
        return {
            'organizations_processed': len(ORG_LIST),
            'cache_updated_at': datetime.now().isoformat()
        }
    
    except Exception as e:
        logger.error(f"Failed to update dashboard data: {str(e)}")
        raise

def update_alert_data(**kwargs):
    """Update the alert data in the cache."""
    try:
        s3_hook = S3Hook()
        redis_hook = RedisHook(redis_conn_id='redis_default')
        redis_conn = redis_hook.get_conn()
        
        # For each organization, load the latest alerts
        all_alerts = []
        
        for org in ORG_LIST:
            # Get today's date
            today = datetime.now().strftime('%Y-%m-%d')
            yesterday = (datetime.now() - timedelta(days=1)).strftime('%Y-%m-%d')
            
            # Try to load today's alerts, then yesterday's if not available
            org_alerts = []
            for date in [today, yesterday]:
                alerts_key = f"{S3_PREFIX}/alerts/{org}/{date}_alerts.json"
                try:
                    alerts_str = s3_hook.read_key(
                        key=alerts_key,
                        bucket_name=S3_BUCKET
                    )
                    org_alerts = json.loads(alerts_str)
                    break
                except Exception as e:
                    logger.warning(f"Could not load alerts for {org} on {date}: {str(e)}")
            
            # Store the alerts in cache
            if org_alerts:
                redis_conn.set(
                    f"{REDIS_CACHE_KEY_PREFIX}:alerts:{org}",
                    json.dumps(org_alerts),
                    ex=86400  # Expire after 24 hours
                )
                all_alerts.extend(org_alerts)
        
        # Store all alerts
        redis_conn.set(
            f"{REDIS_CACHE_KEY_PREFIX}:alerts:all",
            json.dumps(all_alerts),
            ex=86400  # Expire after 24 hours
        )
        
        return {
            'total_alerts': len(all_alerts),
            'organizations_with_alerts': len(set(alert['organization'] for alert in all_alerts))
        }
    
    except Exception as e:
        logger.error(f"Failed to update alert data: {str(e)}")
        raise

# Define the tasks
update_dashboard_task = PythonOperator(
    task_id='update_dashboard_data',
    python_callable=update_dashboard_data,
    dag=dag,
)

update_alerts_task = PythonOperator(
    task_id='update_alert_data',
    python_callable=update_alert_data,
    dag=dag,
)

list_reports_task = PythonOperator(
    task_id='list_available_reports',
    python_callable=list_available_reports,
    dag=dag,
)

# Set dependencies
update_dashboard_task >> update_alerts_task >> list_reports_task

if __name__ == "__main__":
    dag.cli()