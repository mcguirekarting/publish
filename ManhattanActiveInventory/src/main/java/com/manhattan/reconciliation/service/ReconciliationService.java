package com.manhattan.reconciliation.service;

import com.manhattan.reconciliation.model.InventoryRecord;
import com.manhattan.reconciliation.model.ReconciliationHistory;
import com.manhattan.reconciliation.model.ReconciliationResult;
import com.manhattan.reconciliation.model.ReconciliationStatus;
import com.manhattan.reconciliation.model.SystemType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for inventory reconciliation operations.
 */
public interface ReconciliationService {
    
    /**
     * Reconciles inventory for a specific item at a specific location.
     *
     * @param itemId The item ID to reconcile
     * @param locationId The location ID to reconcile
     * @return The result of the reconciliation operation
     */
    ReconciliationResult reconcileInventory(String itemId, String locationId);
    
    /**
     * Gets the reconciliation history for a specific item at a specific location.
     *
     * @param itemId The item ID to get history for
     * @param locationId The location ID to get history for
     * @return A list of reconciliation history records
     */
    List<ReconciliationHistory> getReconciliationHistory(String itemId, String locationId);
    
    /**
     * Gets all reconciliation history records.
     *
     * @return A list of all reconciliation history records
     */
    List<ReconciliationHistory> getAllReconciliationHistory();
    
    /**
     * Gets inventory data from both MAO and MAWM systems.
     *
     * @param itemId The item ID to get inventory for
     * @param locationId The location ID to get inventory for
     * @return A list of inventory records, one from each system that returned data
     */
    List<InventoryRecord> getInventoryFromBothSystems(String itemId, String locationId);
    
    /**
     * Gets inventory data from both systems in a map format.
     * This is for backward compatibility with older tests.
     *
     * @param itemId The item ID to get inventory for
     * @param locationId The location ID to get inventory for
     * @return A map of system name to inventory data
     */
    Map<String, Object> getInventoryData(String itemId, String locationId);
    
    /**
     * Gets a reconciliation history record by its ID.
     *
     * @param id The ID to look up
     * @return An optional containing the record if found
     */
    Optional<ReconciliationHistory> getReconciliationHistoryById(String id);
    
    /**
     * Get a ReconciliationResult by its ID.
     *
     * @param id The ID to look up
     * @return An optional containing the ReconciliationResult if found
     */
    Optional<ReconciliationResult> getReconciliationResultById(Long id);
    
    /**
     * Gets all unresolved (pending) reconciliation results that need approval.
     *
     * @return A list of reconciliation results requiring approval
     */
    List<ReconciliationResult> getPendingReconciliations();
    
    /**
     * Gets all reconciliation results with a specific status.
     *
     * @param status The status to filter by
     * @return A list of reconciliation results with the specified status
     */
    List<ReconciliationResult> getReconciliationsByStatus(ReconciliationStatus status);
    
    /**
     * Approves a reconciliation and updates the inventory in the non-authoritative system.
     *
     * @param reconciliationId The ID of the reconciliation result to approve
     * @param approverNotes Additional notes from the approver
     * @return The updated reconciliation result
     */
    ReconciliationResult approveReconciliation(Long reconciliationId, String approverNotes);
    
    /**
     * Rejects a reconciliation without updating any inventory.
     *
     * @param reconciliationId The ID of the reconciliation result to reject
     * @param rejectionReason The reason for rejection
     * @param overrideAuthority Optional override of the system of authority (can be null to use default)
     * @return The updated reconciliation result
     */
    ReconciliationResult rejectReconciliation(Long reconciliationId, String rejectionReason, SystemType overrideAuthority);
    
    /**
     * Gets all pending reconciliations that have been created after a given date/time.
     *
     * @param since The date/time cutoff
     * @return A list of reconciliation results requiring attention
     */
    List<ReconciliationResult> getPendingReconciliationsSince(LocalDateTime since);
    
    /**
     * Gets all unresolved reconciliation issues.
     * This is for backward compatibility with older tests.
     *
     * @return A list of unresolved reconciliation results
     */
    List<ReconciliationResult> getUnresolvedReconciliationIssues();
}
