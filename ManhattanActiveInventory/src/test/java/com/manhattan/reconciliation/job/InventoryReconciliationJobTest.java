package com.manhattan.reconciliation.job;

import com.manhattan.reconciliation.config.AuthorityConfig;
import com.manhattan.reconciliation.config.ReconciliationJobConfig;
import com.manhattan.reconciliation.model.InventoryItem;
import com.manhattan.reconciliation.model.ReconciliationResult;
import com.manhattan.reconciliation.repository.ReconciliationRepository;
import com.manhattan.reconciliation.service.api.ReconciliationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryReconciliationJobTest {

    @Mock
    private ReconciliationService reconciliationService;
    
    @Mock
    private ReconciliationRepository reconciliationRepository;
    
    @Mock
    private ReconciliationJobConfig jobConfig;
    
    @Mock
    private AuthorityConfig authorityConfig;
    
    @InjectMocks
    private InventoryReconciliationJob reconciliationJob;
    
    private final List<InventoryItem> testItems = Arrays.asList(
            new InventoryItem("ITEM001", "STORE001"),
            new InventoryItem("ITEM002", "STORE002"),
            new InventoryItem("ITEM003", "STORE003")
    );
    
    @BeforeEach
    public void setup() {
        when(jobConfig.isEnabled()).thenReturn(true);
        when(jobConfig.getBatchSize()).thenReturn(10);
        when(reconciliationService.getInventoryItemsForReconciliation(anyInt())).thenReturn(testItems);
    }
    
    @Test
    @DisplayName("Scheduled reconciliation should process all items in batch")
    public void testPerformScheduledReconciliation() {
        // Arrange
        ReconciliationResult successResult = new ReconciliationResult();
        successResult.setReconciled(true);
        
        when(reconciliationService.reconcileInventory(anyString(), anyString())).thenReturn(successResult);
        
        // Act
        reconciliationJob.performScheduledReconciliation();
        
        // Assert
        verify(reconciliationService, times(testItems.size())).reconcileInventory(anyString(), anyString());
        verify(reconciliationService).getInventoryItemsForReconciliation(jobConfig.getBatchSize());
    }
    
    @Test
    @DisplayName("Job should not run when disabled")
    public void testJobDisabled() {
        // Arrange
        when(jobConfig.isEnabled()).thenReturn(false);
        
        // Act
        reconciliationJob.performScheduledReconciliation();
        
        // Assert
        verify(reconciliationService, never()).getInventoryItemsForReconciliation(anyInt());
        verify(reconciliationService, never()).reconcileInventory(anyString(), anyString());
    }
    
    @Test
    @DisplayName("Job should handle exceptions during reconciliation")
    public void testHandleReconciliationException() {
        // Arrange - first item succeeds, second throws exception, third succeeds
        when(reconciliationService.reconcileInventory("ITEM001", "STORE001")).thenReturn(new ReconciliationResult());
        when(reconciliationService.reconcileInventory("ITEM002", "STORE002")).thenThrow(new RuntimeException("Test exception"));
        when(reconciliationService.reconcileInventory("ITEM003", "STORE003")).thenReturn(new ReconciliationResult());
        
        // Act
        reconciliationJob.performScheduledReconciliation();
        
        // Assert - should continue processing after exception
        verify(reconciliationService).reconcileInventory("ITEM001", "STORE001");
        verify(reconciliationService).reconcileInventory("ITEM002", "STORE002");
        verify(reconciliationService).reconcileInventory("ITEM003", "STORE003");
    }
}