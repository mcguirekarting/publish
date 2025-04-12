package com.manhattan.reconciliation.controller;

import com.manhattan.reconciliation.model.InventoryRecord;
import com.manhattan.reconciliation.model.ReconciliationResult;
import com.manhattan.reconciliation.model.SystemType;
import com.manhattan.reconciliation.service.api.ReconciliationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
public class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReconciliationService reconciliationService;

    private final String TEST_ITEM_ID = "ITEM001";
    private final String TEST_LOCATION_ID = "STORE001";

    @Test
    @DisplayName("Should reconcile inventory for item and location")
    public void testReconcileInventory() throws Exception {
        // Arrange
        ReconciliationResult result = new ReconciliationResult();
        result.setId(1L);
        result.setItemId(TEST_ITEM_ID);
        result.setLocationId(TEST_LOCATION_ID);
        result.setMaoQuantity(100);
        result.setMawmQuantity(100);
        result.setDiscrepancy(0);
        result.setAuthoritySystem(SystemType.MAO);
        result.setAutoResolved(true);
        result.setReconciled(true);
        result.setReconciliationMessage("No discrepancy detected");
        result.setReconciliationTime(LocalDateTime.now());
        
        when(reconciliationService.reconcileInventory(TEST_ITEM_ID, TEST_LOCATION_ID)).thenReturn(result);
        
        // Act & Assert
        mockMvc.perform(post("/api/inventory/reconcile")
                .param("itemId", TEST_ITEM_ID)
                .param("locationId", TEST_LOCATION_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemId", is(TEST_ITEM_ID)))
                .andExpect(jsonPath("$.locationId", is(TEST_LOCATION_ID)))
                .andExpect(jsonPath("$.maoQuantity", is(100)))
                .andExpect(jsonPath("$.mawmQuantity", is(100)))
                .andExpect(jsonPath("$.discrepancy", is(0)))
                .andExpect(jsonPath("$.autoResolved", is(true)))
                .andExpect(jsonPath("$.reconciled", is(true)))
                .andExpect(jsonPath("$.reconciliationMessage", is("No discrepancy detected")));
        
        verify(reconciliationService).reconcileInventory(TEST_ITEM_ID, TEST_LOCATION_ID);
    }

    @Test
    @DisplayName("Should get current inventory for item and location")
    public void testGetCurrentInventory() throws Exception {
        // Arrange
        InventoryRecord maoRecord = new InventoryRecord(
                TEST_ITEM_ID, TEST_LOCATION_ID, 100, 20, SystemType.MAO, LocalDateTime.now()
        );
        
        InventoryRecord mawmRecord = new InventoryRecord(
                TEST_ITEM_ID, TEST_LOCATION_ID, 95, 25, SystemType.MAWM, LocalDateTime.now()
        );
        
        List<InventoryRecord> records = Arrays.asList(maoRecord, mawmRecord);
        
        when(reconciliationService.getInventoryData(TEST_ITEM_ID, TEST_LOCATION_ID)).thenReturn(records);
        
        // Act & Assert
        mockMvc.perform(get("/api/inventory/current")
                .param("itemId", TEST_ITEM_ID)
                .param("locationId", TEST_LOCATION_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].itemId", is(TEST_ITEM_ID)))
                .andExpect(jsonPath("$[0].locationId", is(TEST_LOCATION_ID)))
                .andExpect(jsonPath("$[0].quantity", is(100)))
                .andExpect(jsonPath("$[0].systemType", is("MAO")))
                .andExpect(jsonPath("$[1].itemId", is(TEST_ITEM_ID)))
                .andExpect(jsonPath("$[1].locationId", is(TEST_LOCATION_ID)))
                .andExpect(jsonPath("$[1].quantity", is(95)))
                .andExpect(jsonPath("$[1].systemType", is("MAWM")));
        
        verify(reconciliationService).getInventoryData(TEST_ITEM_ID, TEST_LOCATION_ID);
    }

    @Test
    @DisplayName("Should get reconciliation history for time period")
    public void testGetReconciliationHistory() throws Exception {
        // Arrange
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = LocalDateTime.now();
        
        ReconciliationResult result1 = new ReconciliationResult();
        result1.setId(1L);
        result1.setItemId(TEST_ITEM_ID);
        result1.setLocationId(TEST_LOCATION_ID);
        result1.setReconciliationTime(startTime.plusDays(1));
        
        ReconciliationResult result2 = new ReconciliationResult();
        result2.setId(2L);
        result2.setItemId("ITEM002");
        result2.setLocationId("STORE002");
        result2.setReconciliationTime(startTime.plusDays(2));
        
        List<ReconciliationResult> results = Arrays.asList(result1, result2);
        
        when(reconciliationService.getReconciliationHistory(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(results);
        
        // Format dates in ISO format for request parameters
        String startTimeStr = startTime.format(DateTimeFormatter.ISO_DATE_TIME);
        String endTimeStr = endTime.format(DateTimeFormatter.ISO_DATE_TIME);
        
        // Act & Assert
        mockMvc.perform(get("/api/inventory/history")
                .param("startTime", startTimeStr)
                .param("endTime", endTimeStr)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].itemId", is(TEST_ITEM_ID)))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].itemId", is("ITEM002")));
        
        verify(reconciliationService).getReconciliationHistory(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should get unresolved reconciliation issues")
    public void testGetUnresolvedIssues() throws Exception {
        // Arrange
        ReconciliationResult result1 = new ReconciliationResult();
        result1.setId(1L);
        result1.setItemId(TEST_ITEM_ID);
        result1.setLocationId(TEST_LOCATION_ID);
        result1.setDiscrepancy(10);
        result1.setAutoResolved(false);
        result1.setReconciled(false);
        
        ReconciliationResult result2 = new ReconciliationResult();
        result2.setId(2L);
        result2.setItemId("ITEM002");
        result2.setLocationId("STORE002");
        result2.setDiscrepancy(-5);
        result2.setAutoResolved(false);
        result2.setReconciled(false);
        
        List<ReconciliationResult> results = Arrays.asList(result1, result2);
        
        when(reconciliationService.getUnresolvedReconciliationIssues()).thenReturn(results);
        
        // Act & Assert
        mockMvc.perform(get("/api/inventory/issues")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].itemId", is(TEST_ITEM_ID)))
                .andExpect(jsonPath("$[0].discrepancy", is(10)))
                .andExpect(jsonPath("$[0].autoResolved", is(false)))
                .andExpect(jsonPath("$[0].reconciled", is(false)))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].itemId", is("ITEM002")))
                .andExpect(jsonPath("$[1].discrepancy", is(-5)))
                .andExpect(jsonPath("$[1].autoResolved", is(false)))
                .andExpect(jsonPath("$[1].reconciled", is(false)));
        
        verify(reconciliationService).getUnresolvedReconciliationIssues();
    }
}