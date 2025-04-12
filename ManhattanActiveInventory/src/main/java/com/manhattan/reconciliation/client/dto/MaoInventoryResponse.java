package com.manhattan.reconciliation.client.dto;

import java.util.List;

/**
 * Response DTO for Manhattan Active Omni inventory API.
 */
public class MaoInventoryResponse {
    private List<InventoryItem> inventoryItems;
    private String totalResults;

    public MaoInventoryResponse() {
    }

    public List<InventoryItem> getInventoryItems() {
        return inventoryItems;
    }

    public void setInventoryItems(List<InventoryItem> inventoryItems) {
        this.inventoryItems = inventoryItems;
    }

    public String getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(String totalResults) {
        this.totalResults = totalResults;
    }

    public static class InventoryItem {
        private String itemId;
        private String locationId;
        private Integer quantity;
        private SupplyAllocation supplyAllocation;

        public InventoryItem() {
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

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public SupplyAllocation getSupplyAllocation() {
            return supplyAllocation;
        }

        public void setSupplyAllocation(SupplyAllocation supplyAllocation) {
            this.supplyAllocation = supplyAllocation;
        }
    }

    public static class SupplyAllocation {
        private Integer allocatedQuantity;

        public SupplyAllocation() {
        }

        public Integer getAllocatedQuantity() {
            return allocatedQuantity;
        }

        public void setAllocatedQuantity(Integer allocatedQuantity) {
            this.allocatedQuantity = allocatedQuantity;
        }
    }
}
