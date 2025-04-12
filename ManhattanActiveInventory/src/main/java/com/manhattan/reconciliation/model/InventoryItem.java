package com.manhattan.reconciliation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity representing an inventory item that should be reconciled.
 */
@Entity
@IdClass(InventoryItem.InventoryItemId.class)
public class InventoryItem {
    
    @Id
    private String itemId;
    
    @Id
    private String locationId;
    
    private String itemDescription;
    private boolean activeFlag;
    private LocalDateTime lastReconciliationTime;
    private int reconciliationPriority;
    
    /**
     * Composite ID class for InventoryItem.
     */
    public static class InventoryItemId implements Serializable {
        private String itemId;
        private String locationId;
        
        public InventoryItemId() {
        }
        
        public InventoryItemId(String itemId, String locationId) {
            this.itemId = itemId;
            this.locationId = locationId;
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
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            
            InventoryItemId that = (InventoryItemId) o;
            
            if (!itemId.equals(that.itemId)) return false;
            return locationId.equals(that.locationId);
        }
        
        @Override
        public int hashCode() {
            int result = itemId.hashCode();
            result = 31 * result + locationId.hashCode();
            return result;
        }
    }
    
    public InventoryItem() {
    }
    
    public InventoryItem(String itemId, String locationId) {
        this.itemId = itemId;
        this.locationId = locationId;
        this.activeFlag = true; // Default to active
        this.reconciliationPriority = 0; // Default priority
    }
    
    public InventoryItem(String itemId, String locationId, String itemDescription, boolean activeFlag) {
        this.itemId = itemId;
        this.locationId = locationId;
        this.itemDescription = itemDescription;
        this.activeFlag = activeFlag;
        this.reconciliationPriority = 0; // Default priority
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
    
    public String getItemDescription() {
        return itemDescription;
    }
    
    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }
    
    public boolean isActiveFlag() {
        return activeFlag;
    }
    
    public void setActiveFlag(boolean activeFlag) {
        this.activeFlag = activeFlag;
    }
    
    public LocalDateTime getLastReconciliationTime() {
        return lastReconciliationTime;
    }
    
    public void setLastReconciliationTime(LocalDateTime lastReconciliationTime) {
        this.lastReconciliationTime = lastReconciliationTime;
    }
    
    public int getReconciliationPriority() {
        return reconciliationPriority;
    }
    
    public void setReconciliationPriority(int reconciliationPriority) {
        this.reconciliationPriority = reconciliationPriority;
    }
}