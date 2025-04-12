package com.manhattan.reconciliation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Inventory Reconciliation Service.
 * This service reconciles inventory data between Manhattan Active Omni
 * and Manhattan Active Warehouse Management systems.
 */
@SpringBootApplication
@EnableScheduling
public class InventoryReconciliationServiceApplication {

    private static final Logger logger = LoggerFactory.getLogger(InventoryReconciliationServiceApplication.class);

    public static void main(String[] args) {
        try {
            SpringApplication app = new SpringApplication(InventoryReconciliationServiceApplication.class);
            app.run(args);
            logger.info("Application started successfully");
        } catch (Exception e) {
            logger.error("Failed to start application", e);
        }
    }
}