package com.manhattan.reconciliation.client;

import com.manhattan.reconciliation.model.InventoryRecord;

/**
 * Client interface for interacting with the Manhattan Active Omni (MAO) system.
 */
public interface MaoInventoryClient {
    
    /**
     * Gets inventory data for a specific item at a specific location.
     *
     * @param itemId The item ID to get inventory for
     * @param locationId The location ID to get inventory for
     * @return The inventory record from MAO system
     */
    InventoryRecord getInventory(String itemId, String locationId);
    
    /**
     * Fallback method for getInventory in case of circuit breaker triggering.
     *
     * @param itemId The item ID that was requested
     * @param locationId The location ID that was requested
     * @param t The throwable that caused the fallback
     * @return A default inventory record
     */
    default InventoryRecord getInventoryFallback(String itemId, String locationId, RuntimeException t) {
        throw new UnsupportedOperationException("Fallback method not implemented");
    }
    
    /**
     * Updates inventory data for a specific item at a specific location.
     *
     * @param inventoryRecord The inventory record to update
     * @return true if the update was successful, false otherwise
     */
    boolean updateInventory(InventoryRecord inventoryRecord);
    
    /**
     * Fallback method for updateInventory in case of circuit breaker triggering.
     *
     * @param inventoryRecord The inventory record that was being updated
     * @param t The throwable that caused the fallback
     * @return false to indicate failure
     */
    default boolean updateInventoryFallback(InventoryRecord inventoryRecord, RuntimeException t) {
        return false;
    }
}