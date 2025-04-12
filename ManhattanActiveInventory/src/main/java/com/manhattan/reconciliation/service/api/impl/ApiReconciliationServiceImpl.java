package com.manhattan.reconciliation.service.api.impl;

import com.manhattan.reconciliation.model.InventoryItem;
import com.manhattan.reconciliation.model.InventoryRecord;
import com.manhattan.reconciliation.model.ReconciliationResult;
import com.manhattan.reconciliation.repository.ReconciliationResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of the API ReconciliationService interface.
 * Acts as an adapter to the main ReconciliationService implementation.
 */
@Service
public class ApiReconciliationServiceImpl implements com.manhattan.reconciliation.service.api.ReconciliationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiReconciliationServiceImpl.class);
    
    private final com.manhattan.reconciliation.service.ReconciliationService coreReconciliationService;
    private final ReconciliationResultRepository resultRepository;
    
    @Autowired
    public ApiReconciliationServiceImpl(
            com.manhattan.reconciliation.service.ReconciliationService coreReconciliationService,
            ReconciliationResultRepository resultRepository) {
        this.coreReconciliationService = coreReconciliationService;
        this.resultRepository = resultRepository;
    }
    
    @Override
    public ReconciliationResult reconcileInventory(String itemId, String locationId) {
        return coreReconciliationService.reconcileInventory(itemId, locationId);
    }
    
    @Override
    public List<ReconciliationResult> getReconciliationHistory(LocalDateTime startTime, LocalDateTime endTime) {
        logger.info("Getting reconciliation history from {} to {}", startTime, endTime);
        return resultRepository.findByReconciliationTimeBetween(startTime, endTime);
    }
    
    @Override
    public List<ReconciliationResult> getUnresolvedReconciliationIssues() {
        return coreReconciliationService.getUnresolvedReconciliationIssues();
    }
    
    @Override
    public List<InventoryRecord> getInventoryData(String itemId, String locationId) {
        return coreReconciliationService.getInventoryFromBothSystems(itemId, locationId);
    }
    
    @Override
    public List<InventoryItem> getInventoryItemsForReconciliation(int batchSize) {
        logger.info("Getting up to {} inventory items for reconciliation", batchSize);
        // This would typically fetch from a database or integration service
        // For now, return an empty list
        return Collections.emptyList();
    }
}