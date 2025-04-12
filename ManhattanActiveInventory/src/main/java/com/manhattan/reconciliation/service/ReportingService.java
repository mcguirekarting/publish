package com.manhattan.reconciliation.service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface for generating and sending reports.
 */
public interface ReportingService {
    
    /**
     * Generates and sends a daily reconciliation report.
     * 
     * @return true if the report was successfully sent, false otherwise
     */
    boolean sendDailyReconciliationReport();
    
    /**
     * Generates and sends a reconciliation report for a specific date range.
     * 
     * @param startTime the start of the date range
     * @param endTime the end of the date range
     * @param recipients the email addresses to send the report to
     * @return true if the report was successfully sent, false otherwise
     */
    boolean sendReconciliationReport(LocalDateTime startTime, LocalDateTime endTime, List<String> recipients);
}