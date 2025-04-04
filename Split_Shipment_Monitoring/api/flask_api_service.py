"""
Manhattan Active Omni - Split Shipment API Service
-------------------------------------------------
Flask API to serve split shipment data to the dashboard.
"""

from flask import Flask, jsonify, request, send_file, Response
import json
import os
import redis
import boto3
import io
import logging
from datetime import datetime, timedelta
import pandas as pd
from functools import wraps
from werkzeug.security import check_password_hash

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Initialize Flask app
app = Flask(__name__)

# Environment variables
S3_BUCKET = os.environ.get('S3_DATA_BUCKET')
REDIS_HOST = os.environ.get('REDIS_HOST', 'localhost')
REDIS_PORT = int(os.environ.get('REDIS_PORT', 6379))
REDIS_PASSWORD = os.environ.get('REDIS_PASSWORD', '')
REDIS_CACHE_KEY_PREFIX = os.environ.get('REDIS_CACHE_KEY_PREFIX', 'split_shipment')
API_USERNAME = os.environ.get('API_USERNAME', 'admin')
API_PASSWORD_HASH = os.environ.get('API_PASSWORD_HASH')  # Hashed password

# Initialize Redis connection
redis_client = redis.Redis(
    host=REDIS_HOST,
    port=REDIS_PORT,
    password=REDIS_PASSWORD,
    decode_responses=True
)

# Initialize S3 client
s3_client = boto3.client('s3')

# Authentication decorator
def auth_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        auth = request.authorization
        if not auth or not auth.username or not auth.password:
            return authenticate()
        if auth.username != API_USERNAME or not check_password_hash(API_PASSWORD_HASH, auth.password):
            return authenticate()
        return f(*args, **kwargs)
    return decorated

def authenticate():
    """Sends a 401 response that enables basic auth"""
    return Response(
        'Could not verify your access level for that URL.\n'
        'You have to login with proper credentials',
        401,
        {'WWW-Authenticate': 'Basic realm="Login Required"'}
    )

# Health check endpoint
@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint."""
    try:
        # Check Redis connection
        redis_client.ping()
        
        # Return success response
        return jsonify({
            'status': 'healthy',
            'timestamp': datetime.now().isoformat()
        })
    
    except Exception as e:
        logger.error(f"Health check failed: {str(e)}")
        return jsonify({
            'status': 'unhealthy',
            'error': str(e),
            'timestamp': datetime.now().isoformat()
        }), 500

# Get organizations
@app.route('/api/organizations', methods=['GET'])
@auth_required
def get_organizations():
    """Get list of available organizations."""
    try:
        # Get from cache
        orgs = redis_client.get(f"{REDIS_CACHE_KEY_PREFIX}:organizations")
        
        if orgs:
            return jsonify(json.loads(orgs))
        else:
            return jsonify([]), 404
    
    except Exception as e:
        logger.error(f"Failed to get organizations: {str(e)}")
        return jsonify({'error': str(e)}), 500

# Get historical data
@app.route('/api/historical/<organization>', methods=['GET'])
@auth_required
def get_historical_data(organization):
    """Get historical split shipment data for an organization."""
    try:
        # Get time period from query parameters
        period = request.args.get('period', '30days')
        
        # Get from cache
        data = redis_client.get(f"{REDIS_CACHE_KEY_PREFIX}:historical:{period}:{organization}")
        
        if not data:
            # Try to get all historical data
            data = redis_client.get(f"{REDIS_CACHE_KEY_PREFIX}:historical:{organization}")
            
            if not data:
                return jsonify({'error': 'No historical data found'}), 404
            
            # Filter based on period
            df = pd.DataFrame(json.loads(data))
            df['date'] = pd.to_datetime(df['date'])
            
            if period == '7days':
                df = df[df['date'] >= (datetime.now() - timedelta(days=7))]
            elif period == '30days':
                df = df[df['date'] >= (datetime.now() - timedelta(days=30))]
            elif period == '90days':
                df = df[df['date'] >= (datetime.now() - timedelta(days=90))]
            
            data = json.dumps(df.to_dict('records'))
        
        return jsonify(json.loads(data))
    
    except Exception as e:
        logger.error(f"Failed to get historical data: {str(e)}")
        return jsonify({'error': str(e)}), 500

# Get latest analysis
@app.route('/api/analysis/<organization>', methods=['GET'])
@auth_required
def get_analysis(organization):
    """Get latest analysis for an organization."""
    try:
        # Get from cache
        analysis = redis_client.get(f"{REDIS_CACHE_KEY_PREFIX}:analysis:{organization}")
        
        if analysis:
            return jsonify(json.loads(analysis))
        else:
            return jsonify({'error': 'No analysis found'}), 404
    
    except Exception as e:
        logger.error(f"Failed to get analysis: {str(e)}")
        return jsonify({'error': str(e)}), 500

# Get alerts
@app.route('/api/alerts', methods=['GET'])
@auth_required
def get_alerts():
    """Get all active alerts."""
    try:
        # Get organization filter from query parameters
        organization = request.args.get('organization')
        
        if organization:
            # Get alerts for specific organization
            alerts = redis_client.get(f"{REDIS_CACHE_KEY_PREFIX}:alerts:{organization}")
            
            if alerts:
                return jsonify(json.loads(alerts))
            else:
                return jsonify([])
        else:
            # Get all alerts
            alerts = redis_client.get(f"{REDIS_CACHE_KEY_PREFIX}:alerts:all")
            
            if alerts:
                return jsonify(json.loads(alerts))
            else:
                return jsonify([])
    
    except Exception as e:
        logger.error(f"Failed to get alerts: {str(e)}")
        return jsonify({'error': str(e)}), 500

# Get available reports
@app.route('/api/reports', methods=['GET'])
@auth_required
def get_reports():
    """Get available reports."""
    try:
        # Get filters from query parameters
        organization = request.args.get('organization')
        report_type = request.args.get('type')
        date = request.args.get('date')
        
        # Choose the right cache key based on filters
        if organization:
            reports = redis_client.get(f"{REDIS_CACHE_KEY_PREFIX}:reports:by_org:{organization}")
        elif report_type:
            reports = redis_client.get(f"{REDIS_CACHE_KEY_PREFIX}:reports:by_type:{report_type}")
        elif date:
            reports = redis_client.get(f"{REDIS_CACHE_KEY_PREFIX}:reports:by_date:{date}")
        else:
            reports = redis_client.get(f"{REDIS_CACHE_KEY_PREFIX}:reports:all")
        
        if reports:
            reports_data = json.loads(reports)
            
            # Apply additional filters if multiple were specified
            if organization and report_type:
                reports_data = [r for r in reports_data if r.get('organization') == organization and r.get('report_type') == report_type]
            
            if organization and date:
                reports_data = [r for r in reports_data if r.get('organization') == organization and r.get('created_at', '').startswith(date)]
            
            if report_type and date:
                reports_data = [r for r in reports_data if r.get('report_type') == report_type and r.get('created_at', '').startswith(date)]
            
            return jsonify(reports_data)
        else:
            return jsonify([])
    
    except Exception as e:
        logger.error(f"Failed to get reports: {str(e)}")
        return jsonify({'error': str(e)}), 500

# Download report
@app.route('/api/reports/download/<path:report_key>', methods=['GET'])
@auth_required
def download_report(report_key):
    """Download a specific report."""
    try:
        # Get the file from S3
        response = s3_client.get_object(Bucket=S3_BUCKET, Key=report_key)
        
        # Get the filename from the key
        filename = os.path.basename(report_key)
        
        # Determine content type
        content_type = 'application/octet-stream'
        if filename.endswith('.csv'):
            content_type = 'text/csv'
        elif filename.endswith('.pptx'):
            content_type = 'application/vnd.openxmlformats-officedocument.presentationml.presentation'
        
        # Create a file-like object
        file_obj = io.BytesIO(response['Body'].read())
        
        # Return the file
        return send_file(
            file_obj,
            mimetype=content_type,
            as_attachment=True,
            download_name=filename
        )
    
    except Exception as e:
        logger.error(f"Failed to download report: {str(e)}")
        return jsonify({'error': str(e)}), 500

# Dashboard data endpoint - combines multiple data sources for the dashboard
@app.route('/api/dashboard/<organization>', methods=['GET'])
@auth_required
def get_dashboard_data(organization):
    """Get all dashboard data for an organization in a single request."""
    try:
        # Get historical data - last 30 days by default
        period = request.args.get('period', '30days')
        historical_data = redis_client.get(f"{REDIS_CACHE_KEY_PREFIX}:historical:{period}:{organization}")
        
        if not historical_data:
            # Try to get all historical data and filter
            all_historical = redis_client.get(f"{REDIS_CACHE_KEY_PREFIX}:historical:{organization}")
            
            if all_historical:
                df = pd.DataFrame(json.loads(all_historical))
                df['date'] = pd.to_datetime(df['date'])
                
                if period == '7days':
                    df = df[df['date'] >= (datetime.now() - timedelta(days=7))]
                elif period == '30days':
                    df = df[df['date'] >= (datetime.now() - timedelta(days=30))]
                elif period == '90days':
                    df = df[df['date'] >= (datetime.now() - timedelta(days=90))]
                
                historical_data = json.dumps(df.to_dict('records'))
            else:
                historical_data = '[]'
        
        # Get latest analysis
        analysis = redis_client.get(f"{REDIS_CACHE_KEY_PREFIX}:analysis:{organization}")
        if not analysis:
            analysis = '{}'
        
        # Get alerts
        alerts = redis_client.get(f"{REDIS_CACHE_KEY_PREFIX}:alerts:{organization}")
        if not alerts:
            alerts = '[]'
        
        # Get recent reports
        reports = redis_client.get(f"{REDIS_CACHE_KEY_PREFIX}:reports:by_org:{organization}")
        if not reports:
            reports = '[]'
        
        # Combine everything into a single response
        dashboard_data = {
            'historical': json.loads(historical_data),
            'analysis': json.loads(analysis),
            'alerts': json.loads(alerts),
            'reports': json.loads(reports),
            'timestamp': datetime.now().isoformat()
        }
        
        return jsonify(dashboard_data)
    
    except Exception as e:
        logger.error(f"Failed to get dashboard data: {str(e)}")
        return jsonify({'error': str(e)}), 500

# Search data endpoint for complex queries
@app.route('/api/search', methods=['POST'])
@auth_required
def search_data():
    """Search for split shipment data based on complex criteria."""
    try:
        # Get search parameters from request body
        search_params = request.json
        
        if not search_params:
            return jsonify({'error': 'No search parameters provided'}), 400
        
        # Required parameters
        organization = search_params.get('organization')
        
        if not organization:
            return jsonify({'error': 'Organization is required'}), 400
        
        # Optional parameters
        start_date = search_params.get('start_date')
        end_date = search_params.get('end_date')
        location = search_params.get('location')
        item_id = search_params.get('item_id')
        min_split_rate = search_params.get('min_split_rate')
        max_split_rate = search_params.get('max_split_rate')
        
        # Load historical data
        historical_data = redis_client.get(f"{REDIS_CACHE_KEY_PREFIX}:historical:{organization}")
        
        if not historical_data:
            return jsonify({'error': 'No data found for the specified organization'}), 404
        
        # Convert to DataFrame for filtering
        df = pd.DataFrame(json.loads(historical_data))
        
        # Apply filters
        if not df.empty:
            df['date'] = pd.to_datetime(df['date'])
            
            if start_date:
                df = df[df['date'] >= pd.to_datetime(start_date)]
            
            if end_date:
                df = df[df['date'] <= pd.to_datetime(end_date)]
            
            if min_split_rate is not None:
                df = df[df['split_rate'] >= float(min_split_rate)]
            
            if max_split_rate is not None:
                df = df[df['split_rate'] <= float(max_split_rate)]
        
        # For location and item_id filters, we need detailed data
        if location or item_id:
            # Get the analysis with location and SKU data
            analysis = redis_client.get(f"{REDIS_CACHE_KEY_PREFIX}:analysis:{organization}")
            
            if analysis:
                analysis_data = json.loads(analysis)
                
                # Filter by location
                if location and 'location_analysis' in analysis_data:
                    location_data = [loc for loc in analysis_data['location_analysis'] if loc['ship_from_location'] == location]
                    analysis_data['location_analysis'] = location_data
                
                # Filter by item_id (SKU)
                if item_id and 'sku_analysis' in analysis_data:
                    sku_data = [sku for sku in analysis_data['sku_analysis'] if sku['item_id'] == item_id]
                    analysis_data['sku_analysis'] = sku_data
                
                return jsonify({
                    'historical': df.to_dict('records') if not df.empty else [],
                    'analysis': analysis_data,
                    'search_params': search_params
                })
        
        # Return filtered data
        return jsonify({
            'historical': df.to_dict('records') if not df.empty else [],
            'search_params': search_params
        })
    
    except Exception as e:
        logger.error(f"Search failed: {str(e)}")
        return jsonify({'error': str(e)}), 500

# Manhattan API endpoints for direct data access
@app.route('/api/manhattan/auth', methods=['POST'])
@auth_required
def manhattan_auth():
    """Get authentication token for Manhattan Active Omni APIs."""
    try:
        # Get credentials from request
        credentials = request.json
        
        if not credentials:
            return jsonify({'error': 'No credentials provided'}), 400
        
        client_id = credentials.get('client_id')
        client_secret = credentials.get('client_secret')
        manh_base_url = credentials.get('base_url')
        
        if not client_id or not client_secret or not manh_base_url:
            return jsonify({'error': 'Missing required credentials'}), 400
        
        # Make authentication request to Manhattan
        import requests
        
        auth_url = f"{manh_base_url}/oauth/token"
        headers = {'Content-Type': 'application/x-www-form-urlencoded'}
        data = {
            'grant_type': 'client_credentials',
            'client_id': client_id,
            'client_secret': client_secret
        }
        
        response = requests.post(auth_url, headers=headers, data=data)
        response.raise_for_status()
        
        # Return the authentication response
        return jsonify(response.json())
    
    except Exception as e:
        logger.error(f"Manhattan authentication failed: {str(e)}")
        return jsonify({'error': str(e)}), 500

@app.route('/api/manhattan/query', methods=['POST'])
@auth_required
def manhattan_query():
    """Query Manhattan Active Omni APIs directly."""
    try:
        # Get query parameters from request
        query_params = request.json
        
        if not query_params:
            return jsonify({'error': 'No query parameters provided'}), 400
        
        endpoint = query_params.get('endpoint')
        access_token = query_params.get('access_token')
        organization = query_params.get('organization')
        body = query_params.get('body')
        base_url = query_params.get('base_url')
        
        if not endpoint or not access_token or not base_url:
            return jsonify({'error': 'Missing required query parameters'}), 400
        
        # Make request to Manhattan
        import requests
        
        url = f"{base_url}/{endpoint}"
        headers = {
            'Authorization': f'Bearer {access_token}',
            'Content-Type': 'application/json'
        }
        
        if organization:
            headers['Organization'] = organization
        
        if body and request.method == 'POST':
            response = requests.post(url, headers=headers, json=body)
        else:
            response = requests.get(url, headers=headers)
        
        response.raise_for_status()
        
        # Return the response
        return jsonify(response.json())
    
    except Exception as e:
        logger.error(f"Manhattan query failed: {str(e)}")
        return jsonify({'error': str(e)}), 500

# Main entry point
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=int(os.environ.get('PORT', 5000)))