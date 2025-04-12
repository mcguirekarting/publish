package com.manhattan.reconciliation.client.impl;

import com.manhattan.reconciliation.client.MaoInventoryClient;
import com.manhattan.reconciliation.model.InventoryRecord;
import com.manhattan.reconciliation.model.SystemType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Implementation of the MaoInventoryClient interface that interacts with the
 * Manhattan Active Omni (MAO) system.
 */
@Service
public class MaoInventoryClientImpl implements MaoInventoryClient {
    
    private static final Logger logger = LoggerFactory.getLogger(MaoInventoryClientImpl.class);
    
    private final RestTemplate restTemplate;
    private final Random random = new Random();
    
    // For demo purposes, we'll use an in-memory map to simulate the MAO inventory
    private final Map<String, InventoryRecord> inventoryMap = new HashMap<>();
    
    @Value("${manhattan.client.mao.base-url:http://mao-api.example.com}")
    private String maoBaseUrl;
    
    @Value("${manhattan.client.mao.inventory-endpoint:/api/inventory}")
    private String inventoryEndpoint;
    
    public MaoInventoryClientImpl() {
        this.restTemplate = new RestTemplate();
        
        // Add some demo data
        initializeDemoData();
    }
    
    @Override
    @CircuitBreaker(name = "maoInventory", fallbackMethod = "getInventoryFallback")
    public InventoryRecord getInventory(String itemId, String locationId) {
        logger.debug("Getting inventory from MAO for item {} at location {}", itemId, locationId);
        
        // In a real implementation, this would make an API call to the MAO system
        // return restTemplate.getForObject(
        //         maoBaseUrl + inventoryEndpoint + "?itemId={itemId}&locationId={locationId}",
        //         InventoryRecord.class, itemId, locationId);
        
        // For demo purposes, we'll return the data from our in-memory map
        String key = itemId + ":" + locationId;
        InventoryRecord record = inventoryMap.get(key);
        
        if (record == null) {
            // Create a new record with random quantity for demo purposes
            record = new InventoryRecord(
                    SystemType.MAO, 
                    itemId, 
                    locationId, 
                    random.nextInt(100) + 1);
            record.setAllocatedQuantity(random.nextInt(record.getQuantity()));
            inventoryMap.put(key, record);
        }
        
        return record;
    }
    
    public InventoryRecord getInventoryFallback(String itemId, String locationId, Throwable t) {
        logger.warn("Falling back to default inventory for MAO item {} at location {}: {}", 
                itemId, locationId, t.getMessage());
        
        // Return a default record in case of failure
        return new InventoryRecord(SystemType.MAO, itemId, locationId, 0);
    }
    
    @Override
    @CircuitBreaker(name = "maoInventory", fallbackMethod = "updateInventoryFallback")
    public boolean updateInventory(InventoryRecord inventoryRecord) {
        logger.debug("Updating inventory in MAO for item {} at location {} to quantity {}", 
                inventoryRecord.getItemId(), inventoryRecord.getLocationId(), inventoryRecord.getQuantity());
        
        // In a real implementation, this would make an API call to the MAO system
        // restTemplate.put(
        //         maoBaseUrl + inventoryEndpoint,
        //         inventoryRecord);
        
        // For demo purposes, we'll update our in-memory map
        String key = inventoryRecord.getItemId() + ":" + inventoryRecord.getLocationId();
        inventoryRecord.setLastUpdated(LocalDateTime.now());
        inventoryMap.put(key, inventoryRecord);
        
        return true;
    }
    
    public boolean updateInventoryFallback(InventoryRecord inventoryRecord, Throwable t) {
        logger.error("Failed to update inventory in MAO for item {} at location {}: {}", 
                inventoryRecord.getItemId(), inventoryRecord.getLocationId(), t.getMessage());
        
        // Return false to indicate failure
        return false;
    }
    
    private void initializeDemoData() {
        // Add some initial data for demo purposes
        updateInventory(new InventoryRecord(SystemType.MAO, "ITEM001", "STORE001", 100));
        updateInventory(new InventoryRecord(SystemType.MAO, "ITEM002", "STORE001", 50));
        updateInventory(new InventoryRecord(SystemType.MAO, "ITEM001", "STORE002", 75));
        updateInventory(new InventoryRecord(SystemType.MAO, "ITEM002", "STORE002", 25));
    }
}