package com.manhattan.reconciliation.model;

import java.time.LocalDateTime;

/**
 * Model class representing an inventory record from either MAO or MAWM system.
 */
public class InventoryRecord {
    
    private String itemId;
    private String locationId;
    private int quantity;
    private int availableToSell;
    private int allocatedQuantity;
    private SystemType systemType;
    private LocalDateTime lastUpdated;
    
    /**
     * Default constructor.
     */
    public InventoryRecord() {
        // Default constructor
    }
    
    /**
     * Constructor with all fields.
     */
    public InventoryRecord(String itemId, String locationId, int quantity, int availableToSell, 
                           int allocatedQuantity, SystemType systemType, LocalDateTime lastUpdated) {
        this.itemId = itemId;
        this.locationId = locationId;
        this.quantity = quantity;
        this.availableToSell = availableToSell;
        this.allocatedQuantity = allocatedQuantity;
        this.systemType = systemType;
        this.lastUpdated = lastUpdated;
    }
    
    /**
     * Constructor for backward compatibility with tests.
     */
    public InventoryRecord(String itemId, String locationId, int quantity, int availableToSell,
                          SystemType systemType, LocalDateTime lastUpdated) {
        this.itemId = itemId;
        this.locationId = locationId;
        this.quantity = quantity;
        this.availableToSell = availableToSell;
        this.allocatedQuantity = 0;
        this.systemType = systemType;
        this.lastUpdated = lastUpdated;
    }
    
    /**
     * Simplified constructor for basic inventory data.
     */
    public InventoryRecord(SystemType systemType, String itemId, String locationId, int quantity) {
        this.systemType = systemType;
        this.itemId = itemId;
        this.locationId = locationId;
        this.quantity = quantity;
        this.availableToSell = quantity; // Default to same as quantity
        this.allocatedQuantity = 0; // Default to zero allocated
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Getters and setters
    
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
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public int getAvailableToSell() {
        return availableToSell;
    }
    
    public void setAvailableToSell(int availableToSell) {
        this.availableToSell = availableToSell;
    }
    
    public int getAllocatedQuantity() {
        return allocatedQuantity;
    }
    
    public void setAllocatedQuantity(int allocatedQuantity) {
        this.allocatedQuantity = allocatedQuantity;
    }
    
    public SystemType getSystemType() {
        return systemType;
    }
    
    public void setSystemType(SystemType systemType) {
        this.systemType = systemType;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}