package com.manhattan.reconciliation.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for health check endpoints.
 */
@RestController
@RequestMapping("/api/health")
public class HealthCheckController {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckController.class);
    
    /**
     * Health check endpoint that doesn't require database access.
     *
     * @return Health status
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        logger.debug("Health check requested");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "Inventory Reconciliation Service");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Simple status endpoint that returns a plain text message.
     *
     * @return Status message
     */
    @GetMapping("/status")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok("Inventory Reconciliation Service is running");
    }
}