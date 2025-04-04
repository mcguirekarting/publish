"""
Manhattan Active Omni - Split Shipment Analyzer DAG
--------------------------------------------------
This DAG analyzes split shipment data and generates alerts when thresholds are exceeded.
It performs the following steps:
1. Loads split shipment data
2. Analyzes trends and calculates key metrics
3. Checks against threshold values
4. Sends alerts when thresholds are exceeded
"""

from datetime import datetime, timedelta
import json
import pandas as pd
import numpy as np
import logging
from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.models import Variable
from airflow.providers.amazon.aws.hooks.s3 import S3Hook
from airflow.providers.slack.operators.slack_webhook import SlackWebhookOperator
from airflow.providers.amazon.aws.hooks.ses import SesHook

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
    'split_shipment_analyzer',
    default_args=default_args,
    description='Analyzes split shipment data and generates alerts',
    schedule_interval=timedelta(hours=6),
    catchup=False,
    tags=['manhattan', 'split_shipment', 'monitoring', 'alerts'],
)

# Environment variables
ORG_LIST = Variable.get("organization_list", deserialize_json=True)
S3_BUCKET = Variable.get("s3_data_bucket")
S3_PREFIX = "split_shipment_data"
ALERT_THRESHOLDS = Variable.get("alert_thresholds", default=json.dumps({
    'global': {
        'split_rate': 15.0,  # Alert if split rate exceeds 15%
        'increase_rate': 5.0  # Alert if split rate increases by more than 5 percentage points
    },
    'organizations': {}  # Can specify organization-specific thresholds
}), deserialize_json=True)
SLACK_WEBHOOK = Variable.get("slack_webhook_url", default="")
EMAIL_RECIPIENTS = Variable.get("alert_email_recipients", default="[]", deserialize_json=True)

def load_historical_data(organization, **kwargs):
    """Load historical split shipment data for an organization."""
    try:
        s3_hook = S3Hook()
        historical_key = f"{S3_PREFIX}/historical/{organization}_split_rates.json"
        
        try:
            historical_data_str = s3_hook.read_key(
                key=historical_key,
                bucket_name=S3_BUCKET
            )
            historical_data = json.loads(historical_data_str)
        except Exception as e:
            logger.warning(f"Could not load historical data for {organization}: {str(e)}")
            historical_data = []
        
        return historical_data
    
    except Exception as e:
        logger.error(f"Failed to load historical data: {str(e)}")
        raise

def analyze_split_shipment_trends(historical_data, organization, **kwargs):
    """Analyze trends in split shipment data."""
    try:
        if not historical_data:
            logger.warning(f"No historical data available for {organization}")
            return None
        
        # Convert to DataFrame
        df = pd.DataFrame(historical_data)
        
        # Ensure date is in datetime format
        df['date'] = pd.to_datetime(df['date'])
        
        # Sort by date
        df = df.sort_values('date')
        
        # Calculate rolling averages
        df['7_day_avg'] = df['split_rate'].rolling(window=7).mean()
        df['30_day_avg'] = df['split_rate'].rolling(window=30).mean()
        
        # Calculate day-over-day change
        df['previous_day_rate'] = df['split_rate'].shift(1)
        df['day_over_day_change'] = df['split_rate'] - df['previous_day_rate']
        
        # Calculate week-over-week change
        df['previous_week_rate'] = df['split_rate'].shift(7)
        df['week_over_week_change'] = df['split_rate'] - df['previous_week_rate']
        
        # Get the latest data
        latest = df.iloc[-1].to_dict() if not df.empty else {}
        
        # Get last 30 days for trend analysis
        last_30_days = df.tail(30).copy() if len(df) >= 30 else df.copy()
        
        # Calculate trend
        if len(last_30_days) >= 2:
            last_30_days['days_index'] = range(len(last_30_days))
            trend_model = np.polyfit(last_30_days['days_index'], last_30_days['split_rate'], 1)
            trend_slope = trend_model[0]
            trend_direction = "increasing" if trend_slope > 0.1 else "decreasing" if trend_slope < -0.1 else "stable"
        else:
            trend_slope = 0
            trend_direction = "unknown"
        
        # Analyze by fulfillment location (node)
        today = datetime.now().strftime('%Y-%m-%d')
        detailed_key = f"{S3_PREFIX}/detailed/{organization}/{today}_split_shipments.json"
        
        try:
            detailed_data_str = s3_hook.read_key(
                key=detailed_key,
                bucket_name=S3_BUCKET
            )
            detailed_data = json.loads(detailed_data_str)
            detailed_df = pd.DataFrame(detailed_data)
            
            # Group by location and calculate split rates
            location_analysis = detailed_df.groupby('ship_from_location').agg(
                total_lines=('order_line_id', 'count'),
                split_orders=('is_split_shipment', 'sum'),
                total_orders=('order_id', 'nunique')
            )
            location_analysis['split_rate'] = (location_analysis['split_orders'] / location_analysis['total_orders']) * 100
            location_analysis = location_analysis.reset_index()
            location_analysis = location_analysis.sort_values('split_rate', ascending=False)
            
            # Group by SKU and calculate split rates
            sku_analysis = detailed_df.groupby('item_id').agg(
                total_lines=('order_line_id', 'count'),
                split_orders=('is_split_shipment', 'sum'),
                total_orders=('order_id', 'nunique')
            )
            sku_analysis['split_rate'] = (sku_analysis['split_orders'] / sku_analysis['total_orders']) * 100
            sku_analysis = sku_analysis.reset_index()
            sku_analysis = sku_analysis.sort_values('split_rate', ascending=False)
        except Exception as e:
            logger.warning(f"Could not load detailed data for location analysis: {str(e)}")
            location_analysis = pd.DataFrame()
            sku_analysis = pd.DataFrame()
        
        return {
            'organization': organization,
            'latest': latest,
            'trend': {
                'slope': trend_slope,
                'direction': trend_direction
            },
            'location_analysis': location_analysis.to_dict('records') if not location_analysis.empty else [],
            'sku_analysis': sku_analysis.to_dict('records') if not sku_analysis.empty else []
        }
    
    except Exception as e:
        logger.error(f"Failed to analyze split shipment trends: {str(e)}")
        raise

def check_alert_thresholds(analysis_result, **kwargs):
    """Check if any alert thresholds have been exceeded."""
    try:
        if not analysis_result:
            return []
        
        organization = analysis_result['organization']
        latest = analysis_result['latest']
        
        # Get thresholds for this organization
        org_thresholds = ALERT_THRESHOLDS.get('organizations', {}).get(organization, {})
        global_thresholds = ALERT_THRESHOLDS.get('global', {})
        
        # Combine thresholds, with organization-specific ones taking precedence
        thresholds = {**global_thresholds, **org_thresholds}
        
        alerts = []
        
        # Check split rate threshold
        if 'split_rate' in thresholds and 'split_rate' in latest:
            threshold = thresholds['split_rate']
            current_rate = latest['split_rate']
            
            if current_rate > threshold:
                alerts.append({
                    'type': 'split_rate_exceeded',
                    'organization': organization,
                    'threshold': threshold,
                    'current_value': current_rate,
                    'message': f"Split rate of {current_rate:.2f}% exceeds threshold of {threshold:.2f}%"
                })
        
        # Check for significant day-over-day increase
        if 'increase_rate' in thresholds and 'day_over_day_change' in latest:
            threshold = thresholds['increase_rate']
            change = latest['day_over_day_change']
            
            if change > threshold:
                alerts.append({
                    'type': 'daily_increase',
                    'organization': organization,
                    'threshold': threshold,
                    'current_value': change,
                    'message': f"Split rate increased by {change:.2f} percentage points since yesterday, exceeding threshold of {threshold:.2f}"
                })
        
        # Check for significant week-over-week increase
        if 'weekly_increase_rate' in thresholds and 'week_over_week_change' in latest:
            threshold = thresholds['weekly_increase_rate']
            change = latest['week_over_week_change']
            
            if change > threshold:
                alerts.append({
                    'type': 'weekly_increase',
                    'organization': organization,
                    'threshold': threshold,
                    'current_value': change,
                    'message': f"Split rate increased by {change:.2f} percentage points since last week, exceeding threshold of {threshold:.2f}"
                })
        
        # Location-specific alerts
        if 'location_thresholds' in thresholds and analysis_result.get('location_analysis'):
            location_threshold = thresholds['location_thresholds'].get('split_rate', thresholds.get('split_rate', 15.0))
            
            for location in analysis_result['location_analysis']:
                if location['split_rate'] > location_threshold:
                    alerts.append({
                        'type': 'location_split_rate',
                        'organization': organization,
                        'location': location['ship_from_location'],
                        'threshold': location_threshold,
                        'current_value': location['split_rate'],
                        'message': f"Location {location['ship_from_location']} has a split rate of {location['split_rate']:.2f}%, exceeding threshold of {location_threshold:.2f}%"
                    })
        
        # SKU-specific alerts
        if 'sku_thresholds' in thresholds and analysis_result.get('sku_analysis'):
            sku_threshold = thresholds['sku_thresholds'].get('split_rate', thresholds.get('split_rate', 15.0))
            
            for sku in analysis_result['sku_analysis']:
                if sku['split_rate'] > sku_threshold:
                    alerts.append({
                        'type': 'sku_split_rate',
                        'organization': organization,
                        'sku': sku['item_id'],
                        'threshold': sku_threshold,
                        'current_value': sku['split_rate'],
                        'message': f"SKU {sku['item_id']} has a split rate of {sku['split_rate']:.2f}%, exceeding threshold of {sku_threshold:.2f}%"
                    })
        
        return alerts
    
    except Exception as e:
        logger.error(f"Failed to check alert thresholds: {str(e)}")
        raise

def send_alerts(alerts, analysis_result, **kwargs):
    """Send alerts when thresholds are exceeded."""
    try:
        if not alerts:
            logger.info("No alerts to send")
            return
        
        # Group alerts by organization
        alerts_by_org = {}
        for alert in alerts:
            org = alert['organization']
            if org not in alerts_by_org:
                alerts_by_org[org] = []
            alerts_by_org[org].append(alert)
        
        # Prepare and send alerts for each organization
        for org, org_alerts in alerts_by_org.items():
            # Format the alert message
            alert_text = f"*Split Shipment Alerts for {org}*\n\n"
            
            for alert in org_alerts:
                alert_text += f"• {alert['message']}\n"
            
            if analysis_result:
                latest = analysis_result.get('latest', {})
                trend = analysis_result.get('trend', {})
                
                alert_text += f"\n*Current Status:*\n"
                alert_text += f"• Current split rate: {latest.get('split_rate', 'N/A'):.2f}%\n"
                alert_text += f"• 7-day average: {latest.get('7_day_avg', 'N/A'):.2f}%\n"
                alert_text += f"• 30-day average: {latest.get('30_day_avg', 'N/A'):.2f}%\n"
                alert_text += f"• Trend: {trend.get('direction', 'N/A')}\n"
            
            alert_text += f"\n<{Variable.get('dashboard_url', 'http://dashboard-url.example.com')}|View Dashboard>"
            
            # Send to Slack if webhook is configured
            if SLACK_WEBHOOK:
                slack_alert = SlackWebhookOperator(
                    task_id=f'send_slack_alert_{org}',
                    webhook_token=SLACK_WEBHOOK,
                    message=alert_text,
                    username='Split Shipment Monitor',
                    icon_emoji=':warning:',
                    dag=dag
                )
                slack_alert.execute(context=kwargs)
            
            # Send email alerts if recipients are configured
            if EMAIL_RECIPIENTS:
                ses_hook = SesHook()
                for recipient in EMAIL_RECIPIENTS:
                    ses_hook.send_email(
                        source=Variable.get('alert_email_sender', 'noreply@example.com'),
                        to=recipient,
                        subject=f"Split Shipment Alert: {org}",
                        html_content=alert_text.replace('\n', '<br>').replace('*', '<strong>').replace('_', '<em>')
                    )
            
            # Log the alert
            logger.info(f"Sent alerts for {org}: {len(org_alerts)} issues detected")
        
        return True
    
    except Exception as e:
        logger.error(f"Failed to send alerts: {str(e)}")
        raise

def analyze_for_organization(organization, **kwargs):
    """Analyze split shipment data for a specific organization."""
    try:
        # Load historical data
        historical_data = load_historical_data(organization, **kwargs)
        
        # Analyze trends
        analysis_result = analyze_split_shipment_trends(historical_data, organization, **kwargs)
        
        # Check thresholds and generate alerts
        alerts = check_alert_thresholds(analysis_result, **kwargs)
        
        # Send alerts if needed
        if alerts:
            send_alerts(alerts, analysis_result, **kwargs)
        
        # Store the analysis results and alerts in S3
        s3_hook = S3Hook()
        today = datetime.now().strftime('%Y-%m-%d')
        
        # Store analysis results
        if analysis_result:
            analysis_key = f"{S3_PREFIX}/analysis/{organization}/{today}_analysis.json"
            s3_hook.load_string(
                json.dumps(analysis_result),
                key=analysis_key,
                bucket_name=S3_BUCKET,
                replace=True
            )
        
        # Store alerts
        if alerts:
            alerts_key = f"{S3_PREFIX}/alerts/{organization}/{today}_alerts.json"
            s3_hook.load_string(
                json.dumps(alerts),
                key=alerts_key,
                bucket_name=S3_BUCKET,
                replace=True
            )
        
        return {
            'organization': organization,
            'analysis_completed': analysis_result is not None,
            'alerts_generated': len(alerts) if alerts else 0
        }
    
    except Exception as e:
        logger.error(f"Failed to analyze data for organization {organization}: {str(e)}")
        raise

# Create a task for each organization
for org in ORG_LIST:
    org_task = PythonOperator(
        task_id=f'analyze_for_{org}',
        python_callable=analyze_for_organization,
        op_kwargs={'organization': org},
        dag=dag,
    )

if __name__ == "__main__":
    dag.cli()