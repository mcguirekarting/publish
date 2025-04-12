package com.manhattan.reconciliation.controller;

import com.manhattan.reconciliation.model.InventoryRecord;
import com.manhattan.reconciliation.model.ReconciliationResult;
import com.manhattan.reconciliation.service.api.ReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for inventory reconciliation operations.
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);
    
    private final ReconciliationService reconciliationService;
    
    @Autowired
    public InventoryController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }
    
    /**
     * Performs reconciliation for a specific item and location.
     *
     * @param itemId The item ID
     * @param locationId The location ID
     * @return The reconciliation result
     */
    @PostMapping("/reconcile")
    public ResponseEntity<ReconciliationResult> reconcileInventory(
            @RequestParam String itemId,
            @RequestParam String locationId) {
        
        logger.info("Received request to reconcile inventory for item: {} at location: {}", itemId, locationId);
        
        ReconciliationResult result = reconciliationService.reconcileInventory(itemId, locationId);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Gets the current inventory from both systems for a specific item and location.
     *
     * @param itemId The item ID
     * @param locationId The location ID
     * @return List of inventory records
     */
    @GetMapping("/current")
    public ResponseEntity<List<InventoryRecord>> getCurrentInventory(
            @RequestParam String itemId,
            @RequestParam String locationId) {
        
        logger.info("Received request to get current inventory for item: {} at location: {}", itemId, locationId);
        
        List<InventoryRecord> records = reconciliationService.getInventoryData(itemId, locationId);
        
        return ResponseEntity.ok(records);
    }
    
    /**
     * Gets reconciliation history for a specific time period.
     *
     * @param startTime The start time
     * @param endTime The end time
     * @return List of reconciliation results
     */
    @GetMapping("/history")
    public ResponseEntity<List<ReconciliationResult>> getReconciliationHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        logger.info("Received request to get reconciliation history from: {} to: {}", startTime, endTime);
        
        List<ReconciliationResult> results = reconciliationService.getReconciliationHistory(startTime, endTime);
        
        return ResponseEntity.ok(results);
    }
    
    /**
     * Gets unresolved reconciliation issues.
     *
     * @return List of unresolved reconciliation results
     */
    @GetMapping("/issues")
    public ResponseEntity<List<ReconciliationResult>> getUnresolvedIssues() {
        
        logger.info("Received request to get unresolved reconciliation issues");
        
        List<ReconciliationResult> results = reconciliationService.getUnresolvedReconciliationIssues();
        
        return ResponseEntity.ok(results);
    }
}