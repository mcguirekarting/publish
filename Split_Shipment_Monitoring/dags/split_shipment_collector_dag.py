"""
Manhattan Active Omni - Split Shipment Collector DAG
---------------------------------------------------
This DAG collects data about split shipments from Manhattan Active Omni.
It performs the following steps:
1. Authenticates with the Manhattan API
2. Queries for orders with multiple shipments
3. Collects allocation and fulfillment details
4. Stores the data for analysis and reporting
"""

from datetime import datetime, timedelta
import requests
import json
import pandas as pd
import logging
from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.models import Variable
from airflow.providers.amazon.aws.hooks.s3 import S3Hook

# Set up logging
logger = logging.getLogger(__name__)

# Default arguments for the DAG
default_args = {
    'owner': 'airflow',
    'depends_on_past': False,
    'email_on_failure': True,
    'email_on_retry': False,
    'retries': 3,
    'retry_delay': timedelta(minutes=5),
    'start_date': datetime(2023, 1, 1),
}

# Create the DAG
dag = DAG(
    'split_shipment_collector',
    default_args=default_args,
    description='Collects data about split shipments from Manhattan Active Omni',
    schedule_interval=timedelta(hours=6),
    catchup=False,
    tags=['manhattan', 'split_shipment', 'monitoring'],
)

# Environment variables
MANH_BASE_URL = Variable.get("manh_base_url")
MANH_CLIENT_ID = Variable.get("manh_client_id")
MANH_CLIENT_SECRET = Variable.get("manh_client_secret")
ORG_LIST = Variable.get("organization_list", deserialize_json=True)  # List of organizations to check
S3_BUCKET = Variable.get("s3_data_bucket")
S3_PREFIX = "split_shipment_data"

def get_auth_token(**kwargs):
    """Authenticates with Manhattan API and returns an access token."""
    try:
        auth_url = f"{MANH_BASE_URL}/oauth/token"
        headers = {'Content-Type': 'application/x-www-form-urlencoded'}
        data = {
            'grant_type': 'client_credentials',
            'client_id': MANH_CLIENT_ID,
            'client_secret': MANH_CLIENT_SECRET
        }
        
        response = requests.post(auth_url, headers=headers, data=data)
        response.raise_for_status()
        
        token_data = response.json()
        access_token = token_data.get('access_token')
        
        if not access_token:
            raise ValueError("No access token in response")
            
        logger.info("Successfully obtained access token")
        return access_token
    
    except Exception as e:
        logger.error(f"Authentication failed: {str(e)}")
        raise

def query_split_shipments(access_token, organization, **kwargs):
    """Queries for orders with multiple shipments (split shipments)."""
    try:
        # Get current date and yesterday's date
        execution_date = kwargs.get('execution_date', datetime.now())
        yesterday = execution_date - timedelta(days=1)
        yesterday_str = yesterday.strftime('%Y-%m-%dT00:00:00.000')
        today_str = execution_date.strftime('%Y-%m-%dT00:00:00.000')
        
        # Set up request headers
        headers = {
            'Authorization': f'Bearer {access_token}',
            'Content-Type': 'application/json',
            'Organization': organization
        }
        
        # Query for orders created in the last day
        url = f"{MANH_BASE_URL}/order/api/order/order/search"
        data = {
            "Query": f"CreatedTimestamp >= '{yesterday_str}' AND CreatedTimestamp < '{today_str}'",
            "Size": 1000,
            "Sort": {
                "attribute": "CreatedTimestamp",
                "direction": "desc"
            },
            "Template": {
                "OrderId": None,
                "CreatedTimestamp": None,
                "SellingChannel": None,
                "OrderLine": {
                    "OrderLineId": None,
                    "ItemId": None,
                    "DeliveryMethod": {
                        "DeliveryMethodId": None
                    },
                    "Allocation": {
                        "AllocationId": None,
                        "ShipFromLocationId": None
                    },
                    "FulfillmentDetail": {
                        "FulfillmentId": None,
                        "TrackingNumber": None,
                        "CarrierCode": None
                    }
                }
            }
        }
        
        response = requests.post(url, headers=headers, json=data)
        response.raise_for_status()
        
        orders_data = response.json()
        return orders_data
    
    except Exception as e:
        logger.error(f"Failed to query split shipments: {str(e)}")
        raise

def process_split_shipment_data(orders_data, organization, **kwargs):
    """Process the orders data to identify and analyze split shipments."""
    try:
        execution_date = kwargs.get('execution_date', datetime.now())
        date_str = execution_date.strftime('%Y-%m-%d')
        
        # Extract order data
        all_orders = []
        for order in orders_data.get('Results', []):
            order_id = order.get('OrderId')
            created_timestamp = order.get('CreatedTimestamp')
            selling_channel = order.get('SellingChannel')
            
            # Check if this order has multiple order lines with different allocations
            order_lines = order.get('OrderLine', [])
            if not isinstance(order_lines, list):
                order_lines = [order_lines]
            
            # Count unique fulfillment groups for this order
            fulfillment_ids = set()
            for line in order_lines:
                if isinstance(line, dict) and 'FulfillmentDetail' in line:
                    fulfillment_detail = line.get('FulfillmentDetail')
                    if fulfillment_detail and 'FulfillmentId' in fulfillment_detail:
                        fulfillment_ids.add(fulfillment_detail.get('FulfillmentId'))
            
            # If more than one fulfillment, it's a split shipment
            is_split = len(fulfillment_ids) > 1
            
            # For each order line, record the details
            for line in order_lines:
                if not isinstance(line, dict):
                    continue
                
                line_id = line.get('OrderLineId')
                item_id = line.get('ItemId')
                
                delivery_method = line.get('DeliveryMethod', {})
                delivery_method_id = delivery_method.get('DeliveryMethodId') if delivery_method else None
                
                allocation = line.get('Allocation', {})
                allocation_id = allocation.get('AllocationId') if allocation else None
                ship_from_location = allocation.get('ShipFromLocationId') if allocation else None
                
                fulfillment_detail = line.get('FulfillmentDetail', {})
                fulfillment_id = fulfillment_detail.get('FulfillmentId') if fulfillment_detail else None
                tracking_number = fulfillment_detail.get('TrackingNumber') if fulfillment_detail else None
                carrier_code = fulfillment_detail.get('CarrierCode') if fulfillment_detail else None
                
                # Add this order line to our dataset
                all_orders.append({
                    'date': date_str,
                    'organization': organization,
                    'order_id': order_id,
                    'order_line_id': line_id,
                    'created_timestamp': created_timestamp,
                    'selling_channel': selling_channel,
                    'item_id': item_id,
                    'delivery_method': delivery_method_id,
                    'allocation_id': allocation_id,
                    'ship_from_location': ship_from_location,
                    'fulfillment_id': fulfillment_id,
                    'tracking_number': tracking_number,
                    'carrier_code': carrier_code,
                    'is_split_shipment': is_split,
                    'total_fulfillments': len(fulfillment_ids),
                })
        
        # Convert to DataFrame
        df = pd.DataFrame(all_orders)
        
        # Calculate some summary metrics
        total_orders = len(df['order_id'].unique())
        split_orders = len(df[df['is_split_shipment']]['order_id'].unique())
        split_rate = (split_orders / total_orders) * 100 if total_orders > 0 else 0
        
        # Create summary data
        summary_data = {
            'date': date_str,
            'organization': organization,
            'total_orders': total_orders,
            'split_orders': split_orders,
            'split_rate': split_rate
        }
        
        return {'detailed_data': df.to_dict('records'), 'summary': summary_data}
    
    except Exception as e:
        logger.error(f"Failed to process split shipment data: {str(e)}")
        raise

def store_data_in_s3(processed_data, organization, **kwargs):
    """Store the processed data in S3 for later analysis."""
    try:
        execution_date = kwargs.get('execution_date', datetime.now())
        date_str = execution_date.strftime('%Y-%m-%d')
        
        # Connect to S3
        s3_hook = S3Hook()
        
        # Store detailed data
        detailed_data = processed_data['detailed_data']
        detailed_key = f"{S3_PREFIX}/detailed/{organization}/{date_str}_split_shipments.json"
        s3_hook.load_string(
            json.dumps(detailed_data),
            key=detailed_key,
            bucket_name=S3_BUCKET,
            replace=True
        )
        
        # Store summary data
        summary_data = processed_data['summary']
        summary_key = f"{S3_PREFIX}/summary/{organization}/{date_str}_summary.json"
        s3_hook.load_string(
            json.dumps(summary_data),
            key=summary_key,
            bucket_name=S3_BUCKET,
            replace=True
        )
        
        # Add the summary to a historical record
        historical_key = f"{S3_PREFIX}/historical/{organization}_split_rates.json"
        try:
            historical_data_str = s3_hook.read_key(
                key=historical_key,
                bucket_name=S3_BUCKET
            )
            historical_data = json.loads(historical_data_str)
        except:
            historical_data = []
        
        historical_data.append(summary_data)
        s3_hook.load_string(
            json.dumps(historical_data),
            key=historical_key,
            bucket_name=S3_BUCKET,
            replace=True
        )
        
        return {
            'detailed_key': detailed_key,
            'summary_key': summary_key,
            'historical_key': historical_key
        }
    
    except Exception as e:
        logger.error(f"Failed to store data in S3: {str(e)}")
        raise

def collect_for_organization(organization, **kwargs):
    """Collect split shipment data for a specific organization."""
    try:
        ti = kwargs['ti']
        access_token = ti.xcom_pull(task_ids='get_auth_token')
        
        # Query for split shipments
        orders_data = query_split_shipments(access_token, organization, **kwargs)
        
        # Process the data
        processed_data = process_split_shipment_data(orders_data, organization, **kwargs)
        
        # Store the data
        s3_paths = store_data_in_s3(processed_data, organization, **kwargs)
        
        return {
            'organization': organization,
            'data_paths': s3_paths,
            'summary': processed_data['summary']
        }
    
    except Exception as e:
        logger.error(f"Failed to collect data for organization {organization}: {str(e)}")
        raise

# Define the tasks
auth_task = PythonOperator(
    task_id='get_auth_token',
    python_callable=get_auth_token,
    dag=dag,
)

# Create a task for each organization
org_tasks = []
for org in ORG_LIST:
    org_task = PythonOperator(
        task_id=f'collect_for_{org}',
        python_callable=collect_for_organization,
        op_kwargs={'organization': org},
        dag=dag,
    )
    org_tasks.append(org_task)
    auth_task >> org_task

if __name__ == "__main__":
    dag.cli()