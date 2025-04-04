/**
 * API Service for Split Shipment Monitor Dashboard
 * Handles all API calls to the backend service
 */

class SplitShipmentAPI {
    constructor() {
        this.baseUrl = '/api';
        this.authToken = null;
        this.username = 'admin'; // Default username, should be configured
        this.organizationCache = null;
        this.dashboardDataCache = {};
    }

    /**
     * Set the API authentication credentials
     * @param {string} username - API username
     * @param {string} password - API password
     */
    setCredentials(username, password) {
        this.username = username;
        // Create Basic Auth token
        this.authToken = 'Basic ' + btoa(`${username}:${password}`);
        localStorage.setItem('apiUsername', username);
    }

    /**
     * Get the authentication headers for API requests
     * @returns {Object} Headers object with Authorization
     */
    getHeaders() {
        return {
            'Authorization': this.authToken,
            'Content-Type': 'application/json'
        };
    }

    /**
     * Check if the API is available and credentials are valid
     * @returns {Promise<boolean>} True if API is available and credentials are valid
     */
    async checkHealth() {
        try {
            const response = await fetch(`${this.baseUrl}/health`, {
                method: 'GET',
                headers: this.getHeaders()
            });

            return response.ok;
        } catch (error) {
            console.error('API health check failed:', error);
            return false;
        }
    }

    /**
     * Get the list of available organizations
     * @returns {Promise<Array>} List of organization IDs
     */
    async getOrganizations() {
        if (this.organizationCache) {
            return this.organizationCache;
        }

        try {
            const response = await fetch(`${this.baseUrl}/organizations`, {
                method: 'GET',
                headers: this.getHeaders()
            });

            if (!response.ok) {
                throw new Error(`Failed to get organizations: ${response.statusText}`);
            }

            const data = await response.json();
            this.organizationCache = data;
            return data;
        } catch (error) {
            console.error('Failed to get organizations:', error);
            return [];
        }
    }

    /**
     * Get historical split shipment data for an organization
     * @param {string} organization - Organization ID
     * @param {string} period - Time period (7days, 30days, 90days)
     * @returns {Promise<Array>} Historical data
     */
    async getHistoricalData(organization, period = '30days') {
        try {
            const response = await fetch(`${this.baseUrl}/historical/${organization}?period=${period}`, {
                method: 'GET',
                headers: this.getHeaders()
            });

            if (!response.ok) {
                throw new Error(`Failed to get historical data: ${response.statusText}`);
            }

            return await response.json();
        } catch (error) {
            console.error(`Failed to get historical data for ${organization}:`, error);
            return [];
        }
    }

    /**
     * Get the latest analysis for an organization
     * @param {string} organization - Organization ID
     * @returns {Promise<Object>} Analysis data
     */
    async getAnalysis(organization) {
        try {
            const response = await fetch(`${this.baseUrl}/analysis/${organization}`, {
                method: 'GET',
                headers: this.getHeaders()
            });

            if (!response.ok) {
                throw new Error(`Failed to get analysis: ${response.statusText}`);
            }

            return await response.json();
        } catch (error) {
            console.error(`Failed to get analysis for ${organization}:`, error);
            return {};
        }
    }

    /**
     * Get active alerts
     * @param {string} organization - Optional organization filter
     * @returns {Promise<Array>} List of active alerts
     */
    async getAlerts(organization = null) {
        try {
            const url = organization ? 
                `${this.baseUrl}/alerts?organization=${organization}` : 
                `${this.baseUrl}/alerts`;
                
            const response = await fetch(url, {
                method: 'GET',
                headers: this.getHeaders()
            });

            if (!response.ok) {
                throw new Error(`Failed to get alerts: ${response.statusText}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Failed to get alerts:', error);
            return [];
        }
    }

    /**
     * Get available reports
     * @param {Object} filters - Optional filters (organization, type, date)
     * @returns {Promise<Array>} List of available reports
     */
    async getReports(filters = {}) {
        try {
            // Build query string from filters
            const queryParams = new URLSearchParams();
            if (filters.organization) {
                queryParams.append('organization', filters.organization);
            }
            if (filters.type) {
                queryParams.append('type', filters.type);
            }
            if (filters.date) {
                queryParams.append('date', filters.date);
            }

            const queryString = queryParams.toString();
            const url = queryString ? 
                `${this.baseUrl}/reports?${queryString}` : 
                `${this.baseUrl}/reports`;

            const response = await fetch(url, {
                method: 'GET',
                headers: this.getHeaders()
            });

            if (!response.ok) {
                throw new Error(`Failed to get reports: ${response.statusText}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Failed to get reports:', error);
            return [];
        }
    }

    /**
     * Download a report
     * @param {string} reportKey - S3 key for the report
     */
    downloadReport(reportKey) {
        // Create a download link and click it
        const downloadUrl = `${this.baseUrl}/reports/download/${reportKey}`;
        const a = document.createElement('a');
        a.href = downloadUrl;
        a.download = reportKey.split('/').pop();
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
    }

    /**
     * Get all dashboard data for an organization in a single request
     * @param {string} organization - Organization ID
     * @param {string} period - Time period (7days, 30days, 90days)
     * @returns {Promise<Object>} Combined dashboard data
     */
    async getDashboardData(organization, period = '30days') {
        // Check cache first (only if period matches)
        const cacheKey = `${organization}_${period}`;
        if (this.dashboardDataCache[cacheKey] && 
            (Date.now() - this.dashboardDataCache[cacheKey].timestamp) < 300000) { // 5 minutes cache
            return this.dashboardDataCache[cacheKey].data;
        }

        try {
            const response = await fetch(`${this.baseUrl}/dashboard/${organization}?period=${period}`, {
                method: 'GET',
                headers: this.getHeaders()
            });

            if (!response.ok) {
                throw new Error(`Failed to get dashboard data: ${response.statusText}`);
            }

            const data = await response.json();
            
            // Update cache
            this.dashboardDataCache[cacheKey] = {
                data: data,
                timestamp: Date.now()
            };
            
            return data;
        } catch (error) {
            console.error(`Failed to get dashboard data for ${organization}:`, error);
            return {
                historical: [],
                analysis: {},
                alerts: [],
                reports: []
            };
        }
    }

    /**
     * Search for data based on complex criteria
     * @param {Object} searchParams - Search parameters
     * @returns {Promise<Object>} Search results
     */
    async searchData(searchParams) {
        try {
            const response = await fetch(`${this.baseUrl}/search`, {
                method: 'POST',
                headers: this.getHeaders(),
                body: JSON.stringify(searchParams)
            });

            if (!response.ok) {
                throw new Error(`Search failed: ${response.statusText}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Search failed:', error);
            return {
                historical: [],
                analysis: {}
            };
        }
    }
}

// Create a global instance of the API
const splitShipmentAPI = new SplitShipmentAPI();

// Initialize from stored credentials if available
document.addEventListener('DOMContentLoaded', () => {
    const storedUsername = localStorage.getItem('apiUsername');
    if (storedUsername) {
        splitShipmentAPI.username = storedUsername;
    }
    
    // In a real app, you would prompt for password here
    // For this mock-up, we'll assume it's pre-configured
});