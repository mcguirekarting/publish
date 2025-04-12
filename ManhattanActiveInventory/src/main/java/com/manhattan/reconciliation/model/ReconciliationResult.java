package com.manhattan.reconciliation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;

/**
 * Entity representing a reconciliation result.
 */
@Entity
@Table(name = "reconciliation_results")
public class ReconciliationResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
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
    
    @Lob
    @Column(name = "reconciliation_message")
    private String reconciliationMessage;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReconciliationStatus status;
    
    @Column(name = "auto_resolved")
    private boolean autoResolved;
    
    @Column(name = "approved")
    private boolean approved;
    
    @Column(name = "rejected")
    private boolean rejected;
    
    @Column(name = "resolved_time")
    private LocalDateTime resolvedTime;
    
    @Column(name = "approved_by")
    private String approvedBy;
    
    @Column(name = "approver_notes")
    private String approverNotes;
    
    @Column(name = "rejection_reason")
    private String rejectionReason;
    
    @Transient
    private boolean reconciled; // For backward compatibility
    
    public ReconciliationResult() {
        // Default constructor
    }
    
    // Getters and setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
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
    
    public String getReconciliationMessage() {
        return reconciliationMessage;
    }
    
    public void setReconciliationMessage(String reconciliationMessage) {
        this.reconciliationMessage = reconciliationMessage;
    }
    
    public ReconciliationStatus getStatus() {
        return status;
    }
    
    public void setStatus(ReconciliationStatus status) {
        this.status = status;
    }
    
    public boolean isAutoResolved() {
        return autoResolved;
    }
    
    public void setAutoResolved(boolean autoResolved) {
        this.autoResolved = autoResolved;
    }
    
    public boolean isApproved() {
        return approved;
    }
    
    public void setApproved(boolean approved) {
        this.approved = approved;
    }
    
    public boolean isRejected() {
        return rejected;
    }
    
    public void setRejected(boolean rejected) {
        this.rejected = rejected;
    }
    
    public LocalDateTime getResolvedTime() {
        return resolvedTime;
    }
    
    public void setResolvedTime(LocalDateTime resolvedTime) {
        this.resolvedTime = resolvedTime;
    }
    
    public String getApprovedBy() {
        return approvedBy;
    }
    
    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }
    
    public String getApproverNotes() {
        return approverNotes;
    }
    
    public void setApproverNotes(String approverNotes) {
        this.approverNotes = approverNotes;
    }
    
    public String getRejectionReason() {
        return rejectionReason;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
    
    /**
     * For backward compatibility with old tests.
     * @return true if the result is approved, auto-resolved, or rejected
     */
    public boolean isReconciled() {
        return status == ReconciliationStatus.APPROVED || 
               status == ReconciliationStatus.AUTO_RESOLVED || 
               reconciled;
    }
    
    /**
     * For backward compatibility with old tests.
     * @param reconciled the reconciled status
     */
    public void setReconciled(boolean reconciled) {
        this.reconciled = reconciled;
        if (reconciled && status == null) {
            status = ReconciliationStatus.APPROVED;
        }
    }
}