package com.manhattan.reconciliation.client.dto;

/**
 * Request DTO for Manhattan Active Warehouse Management inventory API.
 */
public class MawmInventoryRequest {
    private String itemId;
    private String locationId;

    public MawmInventoryRequest() {
    }

    public MawmInventoryRequest(String itemId, String locationId) {
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
}
