package com.manhattan.reconciliation.repository;

import com.manhattan.reconciliation.model.ReconciliationHistory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for ReconciliationHistory entities.
 * This is the primary implementation that should be used in production.
 */
@Repository
@Primary
public interface ReconciliationHistoryRepository extends JpaRepository<ReconciliationHistory, String> {
    
    /**
     * Finds all reconciliation history records for a specific item at a specific location.
     *
     * @param itemId The item ID to search for
     * @param locationId The location ID to search for
     * @return A list of matching reconciliation history records
     */
    List<ReconciliationHistory> findByItemIdAndLocationId(String itemId, String locationId);
    
    /**
     * Finds all reconciliation history records for a specific result ID.
     *
     * @param resultId The result ID to search for
     * @return A list of matching reconciliation history records
     */
    List<ReconciliationHistory> findByResultId(Long resultId);
    
    /**
     * Finds all reconciliation history records that were created after a specific time.
     *
     * @param time The time after which the reconciliation history records should have been created
     * @return A list of matching reconciliation history records
     */
    List<ReconciliationHistory> findByReconciliationTimeAfter(LocalDateTime time);
    
    /**
     * Finds all reconciliation history records that were created between two times.
     *
     * @param startTime The start of the time range (inclusive)
     * @param endTime The end of the time range (inclusive)
     * @return A list of matching reconciliation history records
     */
    List<ReconciliationHistory> findByReconciliationTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
}