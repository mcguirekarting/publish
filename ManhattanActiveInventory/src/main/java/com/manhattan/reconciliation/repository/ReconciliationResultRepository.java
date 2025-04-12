package com.manhattan.reconciliation.repository;

import com.manhattan.reconciliation.model.ReconciliationResult;
import com.manhattan.reconciliation.model.ReconciliationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for ReconciliationResult entities.
 */
@Repository
public interface ReconciliationResultRepository extends JpaRepository<ReconciliationResult, Long> {
    
    /**
     * Finds all reconciliation results for a specific item at a specific location.
     *
     * @param itemId The item ID to search for
     * @param locationId The location ID to search for
     * @return A list of matching reconciliation results
     */
    List<ReconciliationResult> findByItemIdAndLocationId(String itemId, String locationId);
    
    /**
     * Finds all reconciliation results with a specific status.
     *
     * @param status The status to search for
     * @return A list of reconciliation results with the matching status
     */
    List<ReconciliationResult> findByStatus(ReconciliationStatus status);
    
    /**
     * Finds all reconciliation results with a specific status that were created after a specific time.
     *
     * @param status The status to search for
     * @param time The time after which the reconciliation results should have been created
     * @return A list of matching reconciliation results
     */
    List<ReconciliationResult> findByStatusAndReconciliationTimeAfter(ReconciliationStatus status, LocalDateTime time);
    
    /**
     * Finds all reconciliation results that were created between two times.
     *
     * @param startTime The start of the time range (inclusive)
     * @param endTime The end of the time range (inclusive)
     * @return A list of matching reconciliation results
     */
    List<ReconciliationResult> findByReconciliationTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Counts the number of reconciliation results with a specific status.
     *
     * @param status The status to count
     * @return The number of reconciliation results with the matching status
     */
    long countByStatus(ReconciliationStatus status);
    
    /**
     * Finds all reconciliation results that have not been resolved yet (resolvedTime is null).
     *
     * @return A list of unresolved reconciliation results
     */
    List<ReconciliationResult> findByResolvedTimeIsNull();
}