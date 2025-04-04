/**
 * Main JavaScript file for Split Shipment Monitor Dashboard
 * Initializes the application and handles navigation
 */

// Global state
const appState = {
    currentView: 'dashboard',
    selectedOrganization: null,
    dateRange: '30days',
    selectedNode: '',
    selectedSku: '',
    trendChart: null,
    isLoading: false
};

// DOM elements
const elements = {
    navLinks: {
        dashboard: document.getElementById('nav-dashboard'),
        alerts: document.getElementById('nav-alerts'),
        reports: document.getElementById('nav-reports'),
        settings: document.getElementById('nav-settings')
    },
    views: {
        dashboard: document.getElementById('dashboard-view'),
        alerts: document.getElementById('alerts-view'),
        reports: document.getElementById('reports-view'),
        settings: document.getElementById('settings-view')
    },
    filters: {
        orgSelect: document.getElementById('org-select'),
        dateRange: document.getElementById('date-range'),
        nodeSelect: document.getElementById('node-select'),
        skuInput: document.getElementById('sku-input')
    },
    dashboard: {
        currentSplitRate: document.getElementById('current-split-rate'),
        avgSplitRate7d: document.getElementById('avg-split-rate-7d'),
        avgSplitRate30d: document.getElementById('avg-split-rate-30d'),
        activeAlertsCount: document.getElementById('active-alerts-count'),
        trendChart: document.getElementById('trend-chart'),
        locationTableBody: document.getElementById('location-table-body'),
        skuTableBody: document.getElementById('sku-table-body'),
        exportLocationCsv: document.getElementById('export-location-csv'),
        exportSkuCsv: document.getElementById('export-sku-csv')
    },
    tables: {
        alertsTableBody: document.getElementById('alerts-table-body'),
        reportsTableBody: document.getElementById('reports-table-body')
    },
    lastUpdated: document.getElementById('last-updated')
};

/**
 * Initialize the application
 */
async function initializeApp() {
    // Check API health
    const isHealthy = await splitShipmentAPI.checkHealth();
    if (!isHealthy) {
        // Show error message
        displayError('API service is unavailable. Please check your connection and credentials.');
        return;
    }

    // Load organizations
    const organizations = await splitShipmentAPI.getOrganizations();
    if (organizations.length === 0) {
        displayError('No organizations found. Please check your configuration.');
        return;
    }

    // Populate organization select
    populateOrganizationSelect(organizations);

    // Set default organization
    appState.selectedOrganization = organizations[0];
    elements.filters.orgSelect.value = appState.selectedOrganization;

    // Initialize event listeners
    initEventListeners();

    // Load initial data
    await loadDashboardData();

    // Update last updated timestamp
    updateLastUpdated();

    // Start auto-refresh
    startAutoRefresh();
}