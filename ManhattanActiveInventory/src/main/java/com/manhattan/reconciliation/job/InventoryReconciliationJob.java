package com.manhattan.reconciliation.job;

import com.manhattan.reconciliation.model.ReconciliationResult;
import com.manhattan.reconciliation.model.ReconciliationStatus;
import com.manhattan.reconciliation.service.ReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Job to handle scheduled inventory reconciliation.
 */
@Component
public class InventoryReconciliationJob {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryReconciliationJob.class);
    
    private final ReconciliationService reconciliationService;
    
    @Value("${manhattan.reconciliation.items-to-reconcile:item1,item2}")
    private String itemsToReconcile;
    
    @Value("${manhattan.reconciliation.locations-to-reconcile:loc1,loc2}")
    private String locationsToReconcile;
    
    @Autowired
    public InventoryReconciliationJob(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }
    
    /**
     * Scheduled job to reconcile inventory for configured items and locations.
     * This is scheduled via properties in application.properties.
     */
    @Scheduled(cron = "${manhattan.reconciliation.schedule:0 0 0 * * ?}")
    public void reconcileInventory() {
        performScheduledReconciliation();
    }
    
    /**
     * Performs the scheduled reconciliation. This method is separated from
     * the scheduled method to make it easier to test.
     * 
     * @return A summary of the reconciliation results
     */
    public ReconciliationJobResult performScheduledReconciliation() {
        logger.info("Starting scheduled inventory reconciliation");
        
        String[] items = itemsToReconcile.split(",");
        String[] locations = locationsToReconcile.split(",");
        
        int total = 0;
        int successful = 0;
        int skipped = 0;
        
        for (String itemId : items) {
            for (String locationId : locations) {
                total++;
                try {
                    logger.debug("Reconciling item {} at location {}", itemId, locationId);
                    ReconciliationResult result = reconciliationService.reconcileInventory(itemId, locationId);
                    
                    // Count as successful if it was auto-resolved or if it's pending but we created it
                    if (result.getStatus() == ReconciliationStatus.AUTO_RESOLVED ||
                            result.getStatus() == ReconciliationStatus.PENDING) {
                        successful++;
                    } else {
                        skipped++;
                    }
                } catch (Exception e) {
                    logger.error("Error reconciling item {} at location {}: {}", 
                            itemId, locationId, e.getMessage());
                    skipped++;
                }
            }
        }
        
        logger.info("Completed scheduled inventory reconciliation. Total: {}, Successful: {}, Skipped: {}",
                total, successful, skipped);
        
        return new ReconciliationJobResult(total, successful, skipped);
    }
    
    /**
     * Result class for reconciliation job runs.
     */
    public static class ReconciliationJobResult {
        private final int total;
        private final int successful;
        private final int skipped;
        
        public ReconciliationJobResult(int total, int successful, int skipped) {
            this.total = total;
            this.successful = successful;
            this.skipped = skipped;
        }
        
        public int getTotal() {
            return total;
        }
        
        public int getSuccessful() {
            return successful;
        }
        
        public int getSkipped() {
            return skipped;
        }
    }
}