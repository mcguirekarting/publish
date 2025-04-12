package com.manhattan.reconciliation.client;

import com.manhattan.reconciliation.model.InventoryRecord;
import com.manhattan.reconciliation.model.SystemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MawmInventoryClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private MawmInventoryClient mawmInventoryClient;

    private final String TEST_ITEM_ID = "ITEM001";
    private final String TEST_LOCATION_ID = "STORE001";
    private final String TEST_API_URL = "https://api.manh.com/active-wm";
    private final String TEST_API_TOKEN = "test-token";
    private final String TEST_ORGANIZATION = "test-org";

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(mawmInventoryClient, "mawmApiUrl", TEST_API_URL);
        ReflectionTestUtils.setField(mawmInventoryClient, "mawmApiToken", TEST_API_TOKEN);
        ReflectionTestUtils.setField(mawmInventoryClient, "mawmOrganization", TEST_ORGANIZATION);
    }

    @Test
    @DisplayName("Should get inventory data from MAWM API successfully")
    public void testGetInventorySuccess() {
        // Arrange
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("itemId", TEST_ITEM_ID);
        responseBody.put("locationId", TEST_LOCATION_ID);
        responseBody.put("quantity", 100);
        responseBody.put("allocatedQuantity", 20);
        responseBody.put("lastUpdated", LocalDateTime.now().toString());

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);
        
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        // Act
        InventoryRecord result = mawmInventoryClient.getInventory(TEST_ITEM_ID, TEST_LOCATION_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_ITEM_ID, result.getItemId());
        assertEquals(TEST_LOCATION_ID, result.getLocationId());
        assertEquals(100, result.getQuantity());
        assertEquals(20, result.getAllocatedQuantity());
        assertEquals(SystemType.MAWM, result.getSystemType());
        
        verify(restTemplate).exchange(
                contains(TEST_API_URL + "/api/inventory"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        );
    }

    @Test
    @DisplayName("Should use fallback when MAWM API returns error")
    public void testGetInventoryApiError() {
        // Arrange
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR));

        // Act
        InventoryRecord result = mawmInventoryClient.getInventory(TEST_ITEM_ID, TEST_LOCATION_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_ITEM_ID, result.getItemId());
        assertEquals(TEST_LOCATION_ID, result.getLocationId());
        assertEquals(0, result.getQuantity()); // Fallback value
        assertEquals(0, result.getAllocatedQuantity()); // Fallback value
        assertEquals(SystemType.MAWM, result.getSystemType());
    }

    @Test
    @DisplayName("Should use fallback when MAWM API throws exception")
    public void testGetInventoryApiException() {
        // Arrange
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenThrow(new RuntimeException("API Error"));

        // Act
        InventoryRecord result = mawmInventoryClient.getInventoryFallback(TEST_ITEM_ID, TEST_LOCATION_ID, new RuntimeException("API Error"));

        // Assert
        assertNotNull(result);
        assertEquals(TEST_ITEM_ID, result.getItemId());
        assertEquals(TEST_LOCATION_ID, result.getLocationId());
        assertEquals(0, result.getQuantity()); // Fallback value
        assertEquals(0, result.getAllocatedQuantity()); // Fallback value
        assertEquals(SystemType.MAWM, result.getSystemType());
    }

    @Test
    @DisplayName("Should update inventory in MAWM API successfully")
    public void testUpdateInventorySuccess() {
        // Arrange
        InventoryRecord record = new InventoryRecord(
                TEST_ITEM_ID, TEST_LOCATION_ID, 100, 20, SystemType.MAWM, LocalDateTime.now()
        );

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(new HashMap<>(), HttpStatus.OK);
        
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        // Act
        boolean result = mawmInventoryClient.updateInventory(record);

        // Assert
        assertTrue(result);
        
        verify(restTemplate).exchange(
                eq(TEST_API_URL + "/api/inventory"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Map.class)
        );
    }

    @Test
    @DisplayName("Should return false when MAWM API update fails")
    public void testUpdateInventoryFailure() {
        // Arrange
        InventoryRecord record = new InventoryRecord(
                TEST_ITEM_ID, TEST_LOCATION_ID, 100, 20, SystemType.MAWM, LocalDateTime.now()
        );

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(new HashMap<>(), HttpStatus.INTERNAL_SERVER_ERROR);
        
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        // Act
        boolean result = mawmInventoryClient.updateInventory(record);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should use fallback when MAWM API update throws exception")
    public void testUpdateInventoryException() {
        // Arrange
        InventoryRecord record = new InventoryRecord(
                TEST_ITEM_ID, TEST_LOCATION_ID, 100, 20, SystemType.MAWM, LocalDateTime.now()
        );

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenThrow(new RuntimeException("API Error"));

        // Act
        boolean result = mawmInventoryClient.updateInventoryFallback(record, new RuntimeException("API Error"));

        // Assert
        assertFalse(result);
    }
}