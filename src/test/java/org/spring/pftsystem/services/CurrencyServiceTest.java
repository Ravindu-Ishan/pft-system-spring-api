package org.spring.pftsystem.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CurrencyServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CurrencyService currencyService;

    private static final String TEST_API_URL = "http://api.exchangeratesapi.io/v1/convert";
    private static final String TEST_API_KEY = "test_api_key";

    @BeforeEach
    public void setUp() {
        // Use ReflectionTestUtils to set the private fields marked with @Value
        ReflectionTestUtils.setField(currencyService, "API_URL", TEST_API_URL);
        ReflectionTestUtils.setField(currencyService, "API_KEY", TEST_API_KEY);

        // Inject RestTemplate into the service
        ReflectionTestUtils.setField(currencyService, "restTemplate", restTemplate);
    }

    @Test
    public void testConvertCurrency_Success() {
        // Arrange
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("result", 1.2);

        String expectedUrl = TEST_API_URL + "?api_key=" + TEST_API_KEY + "&from=USD&to=EUR&amount=1.0";
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);

        // Act
        Map<String, Object> result = currencyService.convertCurrency("USD", "EUR", 1.0);

        // Assert
        assertEquals(true, result.get("success"));
        assertEquals(1.2, result.get("result"));
    }

    @Test
    public void testConvertCurrency_InvalidApiUrl() {
        // Arrange
        ReflectionTestUtils.setField(currencyService, "API_URL", "http://invalid.url");

        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("Invalid URL"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            currencyService.convertCurrency("USD", "EUR", 1.0);
        });

        assertEquals("Invalid URL", exception.getMessage());
    }

    @Test
    public void testConvertCurrency_InvalidApiKey() {
        // Arrange
        ReflectionTestUtils.setField(currencyService, "API_KEY", "invalid_key");

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", "Invalid API key");

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);

        // Act
        Map<String, Object> result = currencyService.convertCurrency("USD", "EUR", 1.0);

        // Assert
        assertEquals(false, result.get("success"));
        assertEquals("Invalid API key", result.get("error"));
    }
}