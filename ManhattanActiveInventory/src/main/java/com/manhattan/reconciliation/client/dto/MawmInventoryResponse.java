package com.manhattan.reconciliation.client.dto;

import java.util.List;

/**
 * Response DTO for Manhattan Active Warehouse Management inventory API.
 */
public class MawmInventoryResponse {
    private List<InventoryItem> items;

    public MawmInventoryResponse() {
    }

    public List<InventoryItem> getItems() {
        return items;
    }

    public void setItems(List<InventoryItem> items) {
        this.items = items;
    }

    public static class InventoryItem {
        private String itemId;
        private String locationId;
        private Integer onHandQuantity;
        private Integer allocatedQuantity;

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

        public Integer getOnHandQuantity() {
            return onHandQuantity;
        }

        public void setOnHandQuantity(Integer onHandQuantity) {
            this.onHandQuantity = onHandQuantity;
        }

        public Integer getAllocatedQuantity() {
            return allocatedQuantity;
        }

        public void setAllocatedQuantity(Integer allocatedQuantity) {
            this.allocatedQuantity = allocatedQuantity;
        }
    }
}
