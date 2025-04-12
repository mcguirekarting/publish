package com.manhattan.reconciliation.config;

import com.manhattan.reconciliation.model.SystemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AuthorityConfigTest {

    @Test
    @DisplayName("Should initialize with default authority system")
    public void testDefaultInitialization() {
        // Arrange & Act
        AuthorityConfig config = new AuthorityConfig();
        
        // Assert
        assertEquals(SystemType.MAO, config.getDefaultAuthoritySystem());
        assertNotNull(config.getItemOverrides());
        assertTrue(config.getItemOverrides().isEmpty());
        assertNotNull(config.getLocationOverrides());
        assertTrue(config.getLocationOverrides().isEmpty());
    }
    
    @Test
    @DisplayName("Should set and get default authority system")
    public void testSetGetDefaultAuthoritySystem() {
        // Arrange
        AuthorityConfig config = new AuthorityConfig();
        
        // Act
        config.setDefaultAuthoritySystem(SystemType.MAWM);
        
        // Assert
        assertEquals(SystemType.MAWM, config.getDefaultAuthoritySystem());
    }
    
    @Test
    @DisplayName("Should add and retrieve item overrides")
    public void testItemOverrides() {
        // Arrange
        AuthorityConfig config = new AuthorityConfig();
        
        // Act
        config.addItemOverride("ITEM001", SystemType.MAWM);
        config.addItemOverride("ITEM002", SystemType.MAO);
        
        // Assert
        Map<String, SystemType> itemOverrides = config.getItemOverrides();
        assertEquals(2, itemOverrides.size());
        assertEquals(SystemType.MAWM, itemOverrides.get("ITEM001"));
        assertEquals(SystemType.MAO, itemOverrides.get("ITEM002"));
    }
    
    @Test
    @DisplayName("Should add and retrieve location overrides")
    public void testLocationOverrides() {
        // Arrange
        AuthorityConfig config = new AuthorityConfig();
        
        // Act
        config.addLocationOverride("DC001", SystemType.MAWM);
        config.addLocationOverride("STORE001", SystemType.MAO);
        
        // Assert
        Map<String, SystemType> locationOverrides = config.getLocationOverrides();
        assertEquals(2, locationOverrides.size());
        assertEquals(SystemType.MAWM, locationOverrides.get("DC001"));
        assertEquals(SystemType.MAO, locationOverrides.get("STORE001"));
    }
    
    @Test
    @DisplayName("Should update existing item override")
    public void testUpdateItemOverride() {
        // Arrange
        AuthorityConfig config = new AuthorityConfig();
        config.addItemOverride("ITEM001", SystemType.MAWM);
        
        // Act
        config.addItemOverride("ITEM001", SystemType.MAO);
        
        // Assert
        assertEquals(SystemType.MAO, config.getItemOverrides().get("ITEM001"));
    }
    
    @Test
    @DisplayName("Should update existing location override")
    public void testUpdateLocationOverride() {
        // Arrange
        AuthorityConfig config = new AuthorityConfig();
        config.addLocationOverride("DC001", SystemType.MAO);
        
        // Act
        config.addLocationOverride("DC001", SystemType.MAWM);
        
        // Assert
        assertEquals(SystemType.MAWM, config.getLocationOverrides().get("DC001"));
    }
    
    @Test
    @DisplayName("Should set and get item overrides map")
    public void testSetGetItemOverrides() {
        // Arrange
        AuthorityConfig config = new AuthorityConfig();
        Map<String, SystemType> overrides = Map.of(
                "ITEM001", SystemType.MAWM,
                "ITEM002", SystemType.MAO
        );
        
        // Act
        config.setItemOverrides(overrides);
        
        // Assert
        assertEquals(overrides, config.getItemOverrides());
        assertEquals(2, config.getItemOverrides().size());
    }
    
    @Test
    @DisplayName("Should set and get location overrides map")
    public void testSetGetLocationOverrides() {
        // Arrange
        AuthorityConfig config = new AuthorityConfig();
        Map<String, SystemType> overrides = Map.of(
                "DC001", SystemType.MAWM,
                "STORE001", SystemType.MAO
        );
        
        // Act
        config.setLocationOverrides(overrides);
        
        // Assert
        assertEquals(overrides, config.getLocationOverrides());
        assertEquals(2, config.getLocationOverrides().size());
    }
}