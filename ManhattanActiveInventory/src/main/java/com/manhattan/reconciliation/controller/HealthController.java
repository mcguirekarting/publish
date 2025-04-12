package com.manhattan.reconciliation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
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
@RequestMapping("/health")
public class HealthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * Returns the health status of the application.
     * 
     * @return Health information
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> healthInfo = new HashMap<>();
        
        // Basic health info
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", LocalDateTime.now().toString());
        healthInfo.put("service", "Inventory Reconciliation Service");
        
        // Database connectivity check
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            boolean dbConnected = result != null && result == 1;
            healthInfo.put("database", dbConnected ? "Connected" : "Disconnected");
        } catch (Exception e) {
            healthInfo.put("database", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(healthInfo);
    }
    
    /**
     * Returns more detailed health and readiness information.
     * 
     * @return Detailed health information
     */
    @GetMapping("/detail")
    public ResponseEntity<Map<String, Object>> getDetailedHealth() {
        Map<String, Object> healthInfo = new HashMap<>();
        
        // Basic health info
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", LocalDateTime.now().toString());
        healthInfo.put("service", "Inventory Reconciliation Service");
        
        // Database checks
        Map<String, Object> dbInfo = new HashMap<>();
        try {
            Integer tableCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'reconciliation_result'", 
                    Integer.class);
            
            Integer recordCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM reconciliation_result", 
                    Integer.class);
            
            dbInfo.put("connected", true);
            dbInfo.put("schema_valid", tableCount != null && tableCount > 0);
            dbInfo.put("record_count", recordCount);
        } catch (Exception e) {
            dbInfo.put("connected", false);
            dbInfo.put("error", e.getMessage());
        }
        
        healthInfo.put("database", dbInfo);
        
        // JVM info
        Map<String, Object> jvmInfo = new HashMap<>();
        jvmInfo.put("java_version", System.getProperty("java.version"));
        jvmInfo.put("available_processors", Runtime.getRuntime().availableProcessors());
        jvmInfo.put("free_memory_mb", Runtime.getRuntime().freeMemory() / (1024 * 1024));
        jvmInfo.put("total_memory_mb", Runtime.getRuntime().totalMemory() / (1024 * 1024));
        
        healthInfo.put("jvm", jvmInfo);
        
        return ResponseEntity.ok(healthInfo);
    }
}