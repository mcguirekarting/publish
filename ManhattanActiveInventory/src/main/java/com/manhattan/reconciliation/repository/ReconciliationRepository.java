package com.manhattan.reconciliation.repository;

import com.manhattan.reconciliation.model.ReconciliationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for managing reconciliation results.
 * This is an alias for ReconciliationResultRepository for backward compatibility.
 */
@Repository
public interface ReconciliationRepository extends JpaRepository<ReconciliationResult, Long> {
    
    /**
     * Finds reconciliation results within a specific time period.
     *
     * @param startTime The start time
     * @param endTime The end time
     * @return List of reconciliation results in the time period
     */
    List<ReconciliationResult> findByReconciliationTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Finds unresolved reconciliation issues.
     *
     * @return List of unresolved reconciliation results
     */
    List<ReconciliationResult> findByResolvedTimeIsNull();
    
    /**
     * Finds reconciliation results for a specific item.
     *
     * @param itemId The item ID
     * @return List of reconciliation results for the item
     */
    List<ReconciliationResult> findByItemId(String itemId);
    
    /**
     * Finds reconciliation results for a specific location.
     *
     * @param locationId The location ID
     * @return List of reconciliation results for the location
     */
    List<ReconciliationResult> findByLocationId(String locationId);
    
    /**
     * Finds reconciliation results for a specific item at a specific location.
     *
     * @param itemId The item ID
     * @param locationId The location ID
     * @return List of reconciliation results for the item at the location
     */
    List<ReconciliationResult> findByItemIdAndLocationId(String itemId, String locationId);
}