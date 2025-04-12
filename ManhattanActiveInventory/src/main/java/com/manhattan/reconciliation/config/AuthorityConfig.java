package com.manhattan.reconciliation.config;

import com.manhattan.reconciliation.model.SystemType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for determining the system of authority.
 */
@Entity
@Component
public class AuthorityConfig {

    @Id
    private Long id = 1L;
    
    private SystemType defaultAuthoritySystem = SystemType.MAO;
    
    @Transient
    private Map<String, SystemType> itemOverrides = new HashMap<>();
    
    @Transient
    private Map<String, SystemType> locationOverrides = new HashMap<>();
    
    public AuthorityConfig() {
        // Default constructor
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public SystemType getDefaultAuthoritySystem() {
        return defaultAuthoritySystem;
    }
    
    public void setDefaultAuthoritySystem(SystemType defaultAuthoritySystem) {
        this.defaultAuthoritySystem = defaultAuthoritySystem;
    }
    
    public Map<String, SystemType> getItemOverrides() {
        return itemOverrides;
    }
    
    public void setItemOverrides(Map<String, SystemType> itemOverrides) {
        this.itemOverrides = itemOverrides;
    }
    
    public Map<String, SystemType> getLocationOverrides() {
        return locationOverrides;
    }
    
    public void setLocationOverrides(Map<String, SystemType> locationOverrides) {
        this.locationOverrides = locationOverrides;
    }
    
    /**
     * Adds an item-specific override for the authority system.
     *
     * @param itemId The item ID
     * @param systemType The system of authority
     */
    public void addItemOverride(String itemId, SystemType systemType) {
        itemOverrides.put(itemId, systemType);
    }
    
    /**
     * Adds a location-specific override for the authority system.
     *
     * @param locationId The location ID
     * @param systemType The system of authority
     */
    public void addLocationOverride(String locationId, SystemType systemType) {
        locationOverrides.put(locationId, systemType);
    }
}