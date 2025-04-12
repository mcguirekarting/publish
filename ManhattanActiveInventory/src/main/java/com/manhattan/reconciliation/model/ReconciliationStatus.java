package com.manhattan.reconciliation.model;

/**
 * Enum representing the possible statuses of a reconciliation result.
 */
public enum ReconciliationStatus {
    /**
     * Reconciliation is pending approval or rejection.
     */
    PENDING,
    
    /**
     * Reconciliation has been approved and the non-authoritative system has been updated.
     */
    APPROVED,
    
    /**
     * Reconciliation has been rejected and no inventory updates have been performed.
     */
    REJECTED,
    
    /**
     * Reconciliation was automatically resolved because the discrepancy was within the threshold.
     */
    AUTO_RESOLVED
}