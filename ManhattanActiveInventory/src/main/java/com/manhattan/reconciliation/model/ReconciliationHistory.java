package com.manhattan.reconciliation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Entity representing a historical record of inventory reconciliation.
 * This serves as an audit trail of all reconciliation activities.
 */
@Entity
@Table(name = "reconciliation_history")
public class ReconciliationHistory {
    
    @Id
    private String id;
    
    @Column(name = "item_id", nullable = false)
    private String itemId;
    
    @Column(name = "location_id", nullable = false)
    private String locationId;
    
    @Column(name = "mao_quantity")
    private Integer maoQuantity;
    
    @Column(name = "mawm_quantity")
    private Integer mawmQuantity;
    
    @Column(name = "discrepancy")
    private Integer discrepancy;
    
    @Column(name = "reconciled_quantity")
    private Integer reconciledQuantity;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "authority_system")
    private SystemType authoritySystem;
    
    @Column(name = "reconciliation_time", nullable = false)
    private LocalDateTime reconciliationTime;
    
    @Column(name = "result_id")
    private Long resultId;
    
    public ReconciliationHistory() {
        // Default constructor
    }
    
    // Getters and setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getItemId() {
        return itemId;
    }
    
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    public String getLocationId() {
        return locationId;
    }
    
    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }
    
    public Integer getMaoQuantity() {
        return maoQuantity;
    }
    
    public void setMaoQuantity(Integer maoQuantity) {
        this.maoQuantity = maoQuantity;
    }
    
    public Integer getMawmQuantity() {
        return mawmQuantity;
    }
    
    public void setMawmQuantity(Integer mawmQuantity) {
        this.mawmQuantity = mawmQuantity;
    }
    
    public Integer getDiscrepancy() {
        return discrepancy;
    }
    
    public void setDiscrepancy(Integer discrepancy) {
        this.discrepancy = discrepancy;
    }
    
    public Integer getReconciledQuantity() {
        return reconciledQuantity;
    }
    
    public void setReconciledQuantity(Integer reconciledQuantity) {
        this.reconciledQuantity = reconciledQuantity;
    }
    
    public SystemType getAuthoritySystem() {
        return authoritySystem;
    }
    
    public void setAuthoritySystem(SystemType authoritySystem) {
        this.authoritySystem = authoritySystem;
    }
    
    public LocalDateTime getReconciliationTime() {
        return reconciliationTime;
    }
    
    public void setReconciliationTime(LocalDateTime reconciliationTime) {
        this.reconciliationTime = reconciliationTime;
    }
    
    public Long getResultId() {
        return resultId;
    }
    
    public void setResultId(Long resultId) {
        this.resultId = resultId;
    }
}