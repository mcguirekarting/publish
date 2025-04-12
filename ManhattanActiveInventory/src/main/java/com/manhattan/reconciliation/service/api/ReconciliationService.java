package com.manhattan.reconciliation.service.api;

import com.manhattan.reconciliation.model.InventoryItem;
import com.manhattan.reconciliation.model.InventoryRecord;
import com.manhattan.reconciliation.model.ReconciliationResult;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface for inventory reconciliation operations.
 */
public interface ReconciliationService {
    
    /**
     * Reconciles inventory for a specific item and location.
     *
     * @param itemId The item ID to reconcile
     * @param locationId The location ID to reconcile
     * @return The reconciliation result
     */
    ReconciliationResult reconcileInventory(String itemId, String locationId);
    
    /**
     * Gets a list of reconciliation results for the specified time period.
     *
     * @param startTime The start time of the period
     * @param endTime The end time of the period
     * @return List of reconciliation results
     */
    List<ReconciliationResult> getReconciliationHistory(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Gets all unresolved reconciliation issues.
     *
     * @return List of unresolved reconciliation results
     */
    List<ReconciliationResult> getUnresolvedReconciliationIssues();
    
    /**
     * Gets inventory data from both systems for the specified item and location.
     *
     * @param itemId The item ID
     * @param locationId The location ID
     * @return A list of inventory records (one from each system)
     */
    List<InventoryRecord> getInventoryData(String itemId, String locationId);
    
    /**
     * Gets a batch of inventory items for reconciliation.
     *
     * @param batchSize The maximum number of items to return
     * @return List of inventory items to reconcile
     */
    List<InventoryItem> getInventoryItemsForReconciliation(int batchSize);
}