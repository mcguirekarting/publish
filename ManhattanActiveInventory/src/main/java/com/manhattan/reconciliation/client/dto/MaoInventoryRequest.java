package com.manhattan.reconciliation.client.dto;

import java.util.Objects;

/**
 * Request DTO for Manhattan Active Omni inventory API.
 */
public class MaoInventoryRequest {
    private String query;
    private Template template;

    public MaoInventoryRequest() {
        this.template = new Template();
    }

    public MaoInventoryRequest(String itemId, String locationId) {
        this();
        this.query = String.format("ItemId IN ('%s') AND LocationId = '%s'", itemId, locationId);
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    public static class Template {
        private Object itemId;
        private Object locationId;
        private Object quantity;
        private SupplyAllocation supplyAllocation;

        public Template() {
            this.itemId = null;
            this.locationId = null;
            this.quantity = null;
            this.supplyAllocation = new SupplyAllocation();
        }

        public Object getItemId() {
            return itemId;
        }

        public void setItemId(Object itemId) {
            this.itemId = itemId;
        }

        public Object getLocationId() {
            return locationId;
        }

        public void setLocationId(Object locationId) {
            this.locationId = locationId;
        }

        public Object getQuantity() {
            return quantity;
        }

        public void setQuantity(Object quantity) {
            this.quantity = quantity;
        }

        public SupplyAllocation getSupplyAllocation() {
            return supplyAllocation;
        }

        public void setSupplyAllocation(SupplyAllocation supplyAllocation) {
            this.supplyAllocation = supplyAllocation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Template template = (Template) o;
            return Objects.equals(itemId, template.itemId) &&
                    Objects.equals(locationId, template.locationId) &&
                    Objects.equals(quantity, template.quantity) &&
                    Objects.equals(supplyAllocation, template.supplyAllocation);
        }

        @Override
        public int hashCode() {
            return Objects.hash(itemId, locationId, quantity, supplyAllocation);
        }
    }

    public static class SupplyAllocation {
        private Object allocatedQuantity;

        public SupplyAllocation() {
            this.allocatedQuantity = null;
        }

        public Object getAllocatedQuantity() {
            return allocatedQuantity;
        }

        public void setAllocatedQuantity(Object allocatedQuantity) {
            this.allocatedQuantity = allocatedQuantity;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SupplyAllocation that = (SupplyAllocation) o;
            return Objects.equals(allocatedQuantity, that.allocatedQuantity);
        }

        @Override
        public int hashCode() {
            return Objects.hash(allocatedQuantity);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MaoInventoryRequest that = (MaoInventoryRequest) o;
        return Objects.equals(query, that.query) && Objects.equals(template, that.template);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, template);
    }
}
