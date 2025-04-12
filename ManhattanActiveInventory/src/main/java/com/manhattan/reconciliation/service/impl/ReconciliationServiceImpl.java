package com.manhattan.reconciliation.service.impl;

import com.manhattan.reconciliation.client.MaoInventoryClient;
import com.manhattan.reconciliation.client.MawmInventoryClient;
import com.manhattan.reconciliation.model.InventoryItem;
import com.manhattan.reconciliation.model.InventoryRecord;
import com.manhattan.reconciliation.model.ReconciliationHistory;
import com.manhattan.reconciliation.model.ReconciliationResult;
import com.manhattan.reconciliation.model.ReconciliationStatus;
import com.manhattan.reconciliation.model.SystemType;
import com.manhattan.reconciliation.repository.ReconciliationHistoryRepository;
import com.manhattan.reconciliation.repository.ReconciliationResultRepository;
import com.manhattan.reconciliation.service.ReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the ReconciliationService interface.
 * Implements both the standard and API version of the ReconciliationService interface.
 */
@Service
public class ReconciliationServiceImpl implements ReconciliationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReconciliationServiceImpl.class);
    
    private final MaoInventoryClient maoClient;
    private final MawmInventoryClient mawmClient;
    private final ReconciliationHistoryRepository historyRepository;
    private final ReconciliationResultRepository resultRepository;
    
    @Value("${manhattan.reconciliation.auto-approve-threshold:0}")
    private int autoApproveThreshold;
    
    @Value("${manhattan.reconciliation.system-of-authority:MAO}")
    private String defaultSystemOfAuthority;
    
    @Autowired
    public ReconciliationServiceImpl(
            MaoInventoryClient maoClient,
            MawmInventoryClient mawmClient,
            @Qualifier("reconciliationHistoryRepository") ReconciliationHistoryRepository historyRepository,
            ReconciliationResultRepository resultRepository) {
        this.maoClient = maoClient;
        this.mawmClient = mawmClient;
        this.historyRepository = historyRepository;
        this.resultRepository = resultRepository;
    }
    
    /**
     * Scheduled task to run regular reconciliation on all items
     * configured to be reconciled automatically.
     */
    @Scheduled(cron = "${manhattan.reconciliation.schedule:0 0 0 * * ?}")
    public void scheduledReconciliation() {
        logger.info("Starting scheduled inventory reconciliation");
        // Implementation for scheduled reconciliation
        // This would typically fetch a list of items to reconcile and
        // call reconcileInventory for each one
    }
    
    @Override
    @Transactional
    public ReconciliationResult reconcileInventory(String itemId, String locationId) {
        logger.info("Reconciling inventory for item {} at location {}", itemId, locationId);
        
        // Get inventory data from both systems
        List<InventoryRecord> inventoryRecords = getInventoryFromBothSystems(itemId, locationId);
        
        if (inventoryRecords.size() < 2) {
            logger.warn("Unable to get inventory data from both systems for item {} at location {}", 
                    itemId, locationId);
            
            // Create a result indicating the reconciliation could not be completed
            ReconciliationResult result = new ReconciliationResult();
            result.setItemId(itemId);
            result.setLocationId(locationId);
            result.setReconciliationTime(LocalDateTime.now());
            result.setStatus(ReconciliationStatus.PENDING);
            result.setReconciliationMessage("Could not get inventory data from both systems. " +
                    "Retrieved data from " + inventoryRecords.size() + " system(s).");
            
            if (!inventoryRecords.isEmpty()) {
                InventoryRecord record = inventoryRecords.get(0);
                if (record.getSystemType() == SystemType.MAO) {
                    result.setMaoQuantity(record.getQuantity());
                } else {
                    result.setMawmQuantity(record.getQuantity());
                }
            }
            
            return resultRepository.save(result);
        }
        
        // Extract the inventory quantities
        InventoryRecord maoRecord = inventoryRecords.stream()
                .filter(r -> r.getSystemType() == SystemType.MAO)
                .findFirst()
                .orElse(new InventoryRecord(SystemType.MAO, itemId, locationId, 0));
        
        InventoryRecord mawmRecord = inventoryRecords.stream()
                .filter(r -> r.getSystemType() == SystemType.MAWM)
                .findFirst()
                .orElse(new InventoryRecord(SystemType.MAWM, itemId, locationId, 0));
        
        int maoQuantity = maoRecord.getQuantity();
        int mawmQuantity = mawmRecord.getQuantity();
        int discrepancy = maoQuantity - mawmQuantity;
        
        // Determine system of authority
        SystemType authoritySystem = SystemType.valueOf(defaultSystemOfAuthority);
        int reconciledQuantity = authoritySystem == SystemType.MAO ? maoQuantity : mawmQuantity;
        
        // Create the reconciliation result
        ReconciliationResult result = new ReconciliationResult();
        result.setItemId(itemId);
        result.setLocationId(locationId);
        result.setMaoQuantity(maoQuantity);
        result.setMawmQuantity(mawmQuantity);
        result.setDiscrepancy(discrepancy);
        result.setAuthoritySystem(authoritySystem);
        result.setReconciledQuantity(reconciledQuantity);
        result.setReconciliationTime(LocalDateTime.now());
        
        // Handle auto-approval based on threshold
        if (Math.abs(discrepancy) <= autoApproveThreshold) {
            result.setStatus(ReconciliationStatus.AUTO_RESOLVED);
            result.setAutoResolved(true);
            result.setResolvedTime(LocalDateTime.now());
            result.setReconciliationMessage("Auto-resolved. Discrepancy of " + discrepancy + 
                    " is within threshold of " + autoApproveThreshold);
            
            // If there's a discrepancy, we should update the non-authoritative system
            // However, for now we'll just log it
            logger.info("Auto-approving reconciliation with discrepancy of {}", discrepancy);
        } else {
            result.setStatus(ReconciliationStatus.PENDING);
            result.setAutoResolved(false);
            result.setReconciliationMessage("Manual approval required. Discrepancy of " + 
                    discrepancy + " exceeds threshold of " + autoApproveThreshold);
        }
        
        // Save the result
        result = resultRepository.save(result);
        
        // Create and save a history record
        ReconciliationHistory history = new ReconciliationHistory();
        history.setId(UUID.randomUUID().toString());
        history.setItemId(itemId);
        history.setLocationId(locationId);
        history.setMaoQuantity(maoQuantity);
        history.setMawmQuantity(mawmQuantity);
        history.setDiscrepancy(discrepancy);
        history.setAuthoritySystem(authoritySystem);
        history.setReconciledQuantity(reconciledQuantity);
        history.setReconciliationTime(LocalDateTime.now());
        history.setResultId(result.getId());
        
        historyRepository.save(history);
        
        return result;
    }
    
    @Override
    public List<ReconciliationHistory> getReconciliationHistory(String itemId, String locationId) {
        return historyRepository.findByItemIdAndLocationId(itemId, locationId);
    }
    
    @Override
    public List<ReconciliationHistory> getAllReconciliationHistory() {
        return historyRepository.findAll();
    }
    
    @Override
    public List<InventoryRecord> getInventoryFromBothSystems(String itemId, String locationId) {
        List<InventoryRecord> records = new ArrayList<>();
        
        try {
            InventoryRecord maoRecord = maoClient.getInventory(itemId, locationId);
            if (maoRecord != null) {
                records.add(maoRecord);
            }
        } catch (Exception e) {
            logger.error("Error retrieving inventory from MAO for item {} at location {}: {}", 
                    itemId, locationId, e.getMessage());
        }
        
        try {
            InventoryRecord mawmRecord = mawmClient.getInventory(itemId, locationId);
            if (mawmRecord != null) {
                records.add(mawmRecord);
            }
        } catch (Exception e) {
            logger.error("Error retrieving inventory from MAWM for item {} at location {}: {}", 
                    itemId, locationId, e.getMessage());
        }
        
        return records;
    }
    
    @Override
    public Optional<ReconciliationHistory> getReconciliationHistoryById(String id) {
        return historyRepository.findById(id);
    }
    
    @Override
    public Optional<ReconciliationResult> getReconciliationResultById(Long id) {
        return resultRepository.findById(id);
    }
    
    @Override
    public List<ReconciliationResult> getPendingReconciliations() {
        return resultRepository.findByStatus(ReconciliationStatus.PENDING);
    }
    
    @Override
    public List<ReconciliationResult> getReconciliationsByStatus(ReconciliationStatus status) {
        return resultRepository.findByStatus(status);
    }
    
    @Override
    @Transactional
    public ReconciliationResult approveReconciliation(Long reconciliationId, String approverNotes) {
        Optional<ReconciliationResult> optionalResult = resultRepository.findById(reconciliationId);
        
        if (!optionalResult.isPresent()) {
            throw new IllegalArgumentException("Reconciliation with ID " + reconciliationId + " not found");
        }
        
        ReconciliationResult result = optionalResult.get();
        
        // Can only approve pending reconciliations
        if (result.getStatus() != ReconciliationStatus.PENDING) {
            throw new IllegalStateException("Cannot approve reconciliation with status " + result.getStatus());
        }
        
        // Update the non-authoritative system with the reconciled quantity
        boolean updated = updateNonAuthoritativeSystem(result);
        
        // Update the result
        result.setStatus(ReconciliationStatus.APPROVED);
        result.setApproved(true);
        result.setResolvedTime(LocalDateTime.now());
        result.setApproverNotes(approverNotes);
        
        if (updated) {
            result.setReconciliationMessage(result.getReconciliationMessage() + 
                    "\nApproved and updated non-authoritative system.");
        } else {
            result.setReconciliationMessage(result.getReconciliationMessage() + 
                    "\nApproved but failed to update non-authoritative system.");
        }
        
        return resultRepository.save(result);
    }
    
    @Override
    @Transactional
    public ReconciliationResult rejectReconciliation(Long reconciliationId, String rejectionReason, SystemType overrideAuthority) {
        Optional<ReconciliationResult> optionalResult = resultRepository.findById(reconciliationId);
        
        if (!optionalResult.isPresent()) {
            throw new IllegalArgumentException("Reconciliation with ID " + reconciliationId + " not found");
        }
        
        ReconciliationResult result = optionalResult.get();
        
        // Can only reject pending reconciliations
        if (result.getStatus() != ReconciliationStatus.PENDING) {
            throw new IllegalStateException("Cannot reject reconciliation with status " + result.getStatus());
        }
        
        // If overriding the authority system
        if (overrideAuthority != null && overrideAuthority != result.getAuthoritySystem()) {
            result.setAuthoritySystem(overrideAuthority);
            result.setReconciledQuantity(overrideAuthority == SystemType.MAO ? 
                    result.getMaoQuantity() : result.getMawmQuantity());
            
            result.setReconciliationMessage(result.getReconciliationMessage() + 
                    "\nAuthority system overridden to " + overrideAuthority);
        }
        
        // Update the result
        result.setStatus(ReconciliationStatus.REJECTED);
        result.setRejected(true);
        result.setResolvedTime(LocalDateTime.now());
        result.setRejectionReason(rejectionReason);
        
        result.setReconciliationMessage(result.getReconciliationMessage() + 
                "\nRejected. Reason: " + rejectionReason);
        
        return resultRepository.save(result);
    }
    
    @Override
    public List<ReconciliationResult> getPendingReconciliationsSince(LocalDateTime since) {
        return resultRepository.findByStatusAndReconciliationTimeAfter(
                ReconciliationStatus.PENDING, since);
    }
    
    @Override
    public Map<String, Object> getInventoryData(String itemId, String locationId) {
        List<InventoryRecord> records = getInventoryFromBothSystems(itemId, locationId);
        Map<String, Object> result = new HashMap<>();
        
        for (InventoryRecord record : records) {
            Map<String, Object> recordData = new HashMap<>();
            recordData.put("quantity", record.getQuantity());
            recordData.put("availableToSell", record.getAvailableToSell());
            recordData.put("allocatedQuantity", record.getAllocatedQuantity());
            recordData.put("lastUpdated", record.getLastUpdated().toString());
            
            result.put(record.getSystemType().name(), recordData);
        }
        
        return result;
    }
    
    @Override
    public List<ReconciliationResult> getUnresolvedReconciliationIssues() {
        return resultRepository.findByResolvedTimeIsNull();
    }
    
    /**
     * Updates the inventory in the non-authoritative system to match
     * the authoritative system.
     *
     * @param result The reconciliation result
     * @return true if the update was successful, false otherwise
     */
    private boolean updateNonAuthoritativeSystem(ReconciliationResult result) {
        try {
            if (result.getAuthoritySystem() == SystemType.MAO) {
                // Update MAWM to match MAO
                InventoryRecord record = new InventoryRecord(
                        SystemType.MAWM,
                        result.getItemId(),
                        result.getLocationId(),
                        result.getMaoQuantity());
                mawmClient.updateInventory(record);
            } else {
                // Update MAO to match MAWM
                InventoryRecord record = new InventoryRecord(
                        SystemType.MAO,
                        result.getItemId(),
                        result.getLocationId(),
                        result.getMawmQuantity());
                maoClient.updateInventory(record);
            }
            return true;
        } catch (Exception e) {
            logger.error("Error updating non-authoritative system for reconciliation {}: {}", 
                    result.getId(), e.getMessage());
            return false;
        }
    }
}