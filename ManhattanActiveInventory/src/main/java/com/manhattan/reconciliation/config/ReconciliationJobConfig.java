package com.manhattan.reconciliation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the reconciliation job.
 */
@Configuration
public class ReconciliationJobConfig {
    
    @Value("${manhattan.reconciliation.job.enabled:true}")
    private boolean enabled;
    
    @Value("${manhattan.reconciliation.job.batch-size:100}")
    private int batchSize;
    
    @Value("${manhattan.reconciliation.job.cron:0 0/30 * * * ?}")
    private String cronExpression;
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getBatchSize() {
        return batchSize;
    }
    
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
    
    public String getCronExpression() {
        return cronExpression;
    }
    
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }
}