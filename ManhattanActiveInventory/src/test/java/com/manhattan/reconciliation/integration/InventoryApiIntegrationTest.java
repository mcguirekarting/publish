package com.manhattan.reconciliation.integration;

import com.manhattan.reconciliation.model.InventoryRecord;
import com.manhattan.reconciliation.model.ReconciliationResult;
import com.manhattan.reconciliation.model.SystemType;
import com.manhattan.reconciliation.repository.ReconciliationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class InventoryApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ReconciliationRepository reconciliationRepository;

    private final String TEST_ITEM_ID = "ITEM001";
    private final String TEST_LOCATION_ID = "STORE001";

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/inventory";
    }

    @BeforeEach
    public void setup() {
        // Clear any existing test data
        reconciliationRepository.deleteAll();
    }

    @Test
    public void testGetUnresolvedIssues() {
        // Arrange
        ReconciliationResult resolved = createSampleReconciliationResult("ITEM001", "STORE001", 0);
        resolved.setAutoResolved(true);
        resolved.setReconciled(true);
        
        ReconciliationResult unresolved1 = createSampleReconciliationResult("ITEM002", "STORE002", 10);
        unresolved1.setAutoResolved(false);
        unresolved1.setReconciled(false);
        
        ReconciliationResult unresolved2 = createSampleReconciliationResult("ITEM003", "STORE003", -5);
        unresolved2.setAutoResolved(false);
        unresolved2.setReconciled(false);
        
        reconciliationRepository.save(resolved);
        reconciliationRepository.save(unresolved1);
        reconciliationRepository.save(unresolved2);
        
        // Act
        ResponseEntity<List<ReconciliationResult>> response = restTemplate.exchange(
                getBaseUrl() + "/issues",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ReconciliationResult>>() {}
        );
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertTrue(response.getBody().stream().noneMatch(r -> r.getItemId().equals("ITEM001")));
        assertTrue(response.getBody().stream().allMatch(r -> !r.isAutoResolved() && !r.isReconciled()));
    }

    @Test
    public void testGetReconciliationHistory() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        ReconciliationResult result1 = createSampleReconciliationResult("ITEM001", "STORE001", 5);
        result1.setReconciliationTime(now.minusDays(2));
        
        ReconciliationResult result2 = createSampleReconciliationResult("ITEM002", "STORE002", 0);
        result2.setReconciliationTime(now.minusDays(1));
        
        reconciliationRepository.save(result1);
        reconciliationRepository.save(result2);
        
        LocalDateTime startTime = now.minusDays(3);
        LocalDateTime endTime = now;
        
        // Format dates for URL
        String startTimeStr = startTime.format(DateTimeFormatter.ISO_DATE_TIME);
        String endTimeStr = endTime.format(DateTimeFormatter.ISO_DATE_TIME);
        
        // Build URL with query parameters
        String url = UriComponentsBuilder.fromHttpUrl(getBaseUrl() + "/history")
                .queryParam("startTime", startTimeStr)
                .queryParam("endTime", endTimeStr)
                .toUriString();
        
        // Act
        ResponseEntity<List<ReconciliationResult>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ReconciliationResult>>() {}
        );
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
    }

    @Test
    public void testHealthEndpoint() {
        // Simple test to verify the health endpoint is working
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/health/status", 
                String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Inventory Reconciliation Service is running", response.getBody());
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