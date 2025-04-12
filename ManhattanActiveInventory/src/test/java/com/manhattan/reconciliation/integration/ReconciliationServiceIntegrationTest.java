package com.manhattan.reconciliation.integration;

import com.manhattan.reconciliation.model.ReconciliationResult;
import com.manhattan.reconciliation.model.SystemType;
import com.manhattan.reconciliation.repository.ReconciliationRepository;
import com.manhattan.reconciliation.service.api.ReconciliationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ReconciliationServiceIntegrationTest {

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private ReconciliationRepository reconciliationRepository;

    private final String TEST_ITEM_ID = "ITEM001";
    private final String TEST_LOCATION_ID = "STORE001";

    @Test
    public void testGetReconciliationHistory() {
        // Arrange - Create sample reconciliation results in the database
        ReconciliationResult result1 = createSampleReconciliationResult(TEST_ITEM_ID, TEST_LOCATION_ID, 1);
        ReconciliationResult result2 = createSampleReconciliationResult("ITEM002", "STORE002", 2);
        
        reconciliationRepository.save(result1);
        reconciliationRepository.save(result2);
        
        LocalDateTime startTime = LocalDateTime.now().minusDays(1);
        LocalDateTime endTime = LocalDateTime.now().plusDays(1);
        
        // Act
        List<ReconciliationResult> results = reconciliationService.getReconciliationHistory(startTime, endTime);
        
        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(r -> r.getItemId().equals(TEST_ITEM_ID)));
        assertTrue(results.stream().anyMatch(r -> r.getItemId().equals("ITEM002")));
    }

    @Test
    public void testGetUnresolvedReconciliationIssues() {
        // Arrange - Create unresolved reconciliation results in the database
        ReconciliationResult result1 = createSampleReconciliationResult(TEST_ITEM_ID, TEST_LOCATION_ID, 5);
        result1.setAutoResolved(false);
        result1.setReconciled(false);
        
        ReconciliationResult result2 = createSampleReconciliationResult("ITEM002", "STORE002", -3);
        result2.setAutoResolved(false);
        result2.setReconciled(false);
        
        ReconciliationResult result3 = createSampleReconciliationResult("ITEM003", "STORE003", 0);
        result3.setAutoResolved(true);
        result3.setReconciled(true);
        
        reconciliationRepository.save(result1);
        reconciliationRepository.save(result2);
        reconciliationRepository.save(result3);
        
        // Act
        List<ReconciliationResult> results = reconciliationService.getUnresolvedReconciliationIssues();
        
        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.stream().noneMatch(r -> r.getItemId().equals("ITEM003")));
        assertTrue(results.stream().allMatch(r -> !r.isAutoResolved() && !r.isReconciled()));
    }

    /**
     * Helper method to create a sample reconciliation result for testing.
     */
    private ReconciliationResult createSampleReconciliationResult(String itemId, String locationId, int discrepancy) {
        ReconciliationResult result = new ReconciliationResult();
        result.setItemId(itemId);
        result.setLocationId(locationId);
        result.setMaoQuantity(100);
        result.setMawmQuantity(100 - discrepancy);
        result.setDiscrepancy(discrepancy);
        result.setAuthoritySystem(SystemType.MAO);
        result.setReconciliationTime(LocalDateTime.now());
        result.setAutoResolved(discrepancy == 0);
        result.setReconciled(discrepancy == 0);
        result.setReconciliationMessage(discrepancy == 0 ? "No discrepancy detected" : "Discrepancy detected");
        return result;
    }
}