package org.spring.pftsystem.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.pftsystem.entity.schema.main.SystemSettings;
import org.spring.pftsystem.repository.SystemSettingsRepo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SystemSettingsServiceTest {

    @Mock
    private SystemSettingsRepo systemSettingsRepo;

    @InjectMocks
    private SystemSettingsService systemSettingsService;

    @Test
    void testGetSystemSettings_ExistingSettings() {
        // Arrange
        List<String> categories = Arrays.asList("Food", "Transport", "Entertainment");
        SystemSettings existingSettings = new SystemSettings(
                "existing123",
                2000,
                150,
                categories,
                43200
        );

        when(systemSettingsRepo.findFirstByOrderByIdAsc()).thenReturn(existingSettings);

        // Act
        SystemSettings result = systemSettingsService.getSystemSettings();

        // Assert
        assertNotNull(result);
        assertEquals("existing123", result.getId());
        assertEquals(2000, result.getTotalTransactionsLimit());
        assertEquals(150, result.getRecurringTransactionsLimit());
        assertEquals(categories, result.getCategories());
        assertEquals(43200, result.getJWTExpirationTime());

        // Verify
        verify(systemSettingsRepo, times(1)).findFirstByOrderByIdAsc();
        verify(systemSettingsRepo, never()).save(any(SystemSettings.class));
    }

    @Test
    void testGetSystemSettings_CreateDefaultWhenNull() {
        // Arrange
        SystemSettings defaultSettings = new SystemSettings(
                "default",
                1000,
                100,
                new ArrayList<>(),
                86400
        );

        when(systemSettingsRepo.findFirstByOrderByIdAsc()).thenReturn(null);
        when(systemSettingsRepo.save(any(SystemSettings.class))).thenReturn(defaultSettings);

        // Act
        SystemSettings result = systemSettingsService.getSystemSettings();

        // Assert
        assertNotNull(result);
        assertEquals("default", result.getId());
        assertEquals(1000, result.getTotalTransactionsLimit());
        assertEquals(100, result.getRecurringTransactionsLimit());
        assertTrue(result.getCategories().isEmpty());
        assertEquals(86400, result.getJWTExpirationTime());

        // Verify
        verify(systemSettingsRepo, times(1)).findFirstByOrderByIdAsc();
        verify(systemSettingsRepo, times(1)).save(any(SystemSettings.class));
    }

    @Test
    void testUpdateSystemSettings_Success() {
        // Arrange
        List<String> categories = Arrays.asList("Bills", "Groceries", "Healthcare");
        SystemSettings settingsToUpdate = new SystemSettings(
                "settings123",
                3000,
                200,
                categories,
                172800
        );

        when(systemSettingsRepo.save(any(SystemSettings.class))).thenReturn(settingsToUpdate);

        // Act
        SystemSettings result = systemSettingsService.updateSystemSettings(settingsToUpdate);

        // Assert
        assertNotNull(result);
        assertEquals("settings123", result.getId());
        assertEquals(3000, result.getTotalTransactionsLimit());
        assertEquals(200, result.getRecurringTransactionsLimit());
        assertEquals(categories, result.getCategories());
        assertEquals(172800, result.getJWTExpirationTime());

        // Verify
        verify(systemSettingsRepo, times(1)).save(settingsToUpdate);
    }

    @Test
    void testUpdateSystemSettings_WithNullId() {
        // Arrange
        List<String> categories = Arrays.asList("Utilities", "Rent");
        SystemSettings settingsWithNullId = new SystemSettings(
                null,  // Null ID
                5000,
                300,
                categories,
                259200
        );

        SystemSettings savedSettings = new SystemSettings(
                "generated123",  // Generated ID
                5000,
                300,
                categories,
                259200
        );

        when(systemSettingsRepo.save(any(SystemSettings.class))).thenReturn(savedSettings);

        // Act
        SystemSettings result = systemSettingsService.updateSystemSettings(settingsWithNullId);

        // Assert
        assertNotNull(result);
        assertEquals("generated123", result.getId());
        assertEquals(5000, result.getTotalTransactionsLimit());
        assertEquals(300, result.getRecurringTransactionsLimit());
        assertEquals(categories, result.getCategories());
        assertEquals(259200, result.getJWTExpirationTime());

        // Verify
        verify(systemSettingsRepo, times(1)).save(settingsWithNullId);
    }

    @Test
    void testUpdateSystemSettings_EmptyCategories() {
        // Arrange
        SystemSettings settingsWithEmptyCategories = new SystemSettings(
                "settings456",
                1500,
                120,
                new ArrayList<>(),  // Empty categories list
                43200
        );

        when(systemSettingsRepo.save(any(SystemSettings.class))).thenReturn(settingsWithEmptyCategories);

        // Act
        SystemSettings result = systemSettingsService.updateSystemSettings(settingsWithEmptyCategories);

        // Assert
        assertNotNull(result);
        assertEquals("settings456", result.getId());
        assertEquals(1500, result.getTotalTransactionsLimit());
        assertEquals(120, result.getRecurringTransactionsLimit());
        assertTrue(result.getCategories().isEmpty());
        assertEquals(43200, result.getJWTExpirationTime());

        // Verify
        verify(systemSettingsRepo, times(1)).save(settingsWithEmptyCategories);
    }

    @Test
    void testCreateDefaultSettings_InternalMethod() {
        // This test verifies the behavior of the private method by testing its impact
        // on the public method getSystemSettings when no settings exist

        // Arrange
        SystemSettings defaultSettings = new SystemSettings(
                "default",
                1000,
                100,
                new ArrayList<>(),
                86400
        );

        when(systemSettingsRepo.findFirstByOrderByIdAsc()).thenReturn(null);
        when(systemSettingsRepo.save(any(SystemSettings.class))).thenAnswer(invocation -> {
            SystemSettings settings = invocation.getArgument(0);
            // Verify default values were set correctly
            assertEquals("default", settings.getId());
            assertEquals(1000, settings.getTotalTransactionsLimit());
            assertEquals(100, settings.getRecurringTransactionsLimit());
            assertTrue(settings.getCategories().isEmpty());
            assertEquals(86400, settings.getJWTExpirationTime());
            return defaultSettings;
        });

        // Act
        SystemSettings result = systemSettingsService.getSystemSettings();

        // Assert
        assertNotNull(result);
        assertEquals(defaultSettings, result);

        // Verify
        verify(systemSettingsRepo, times(1)).findFirstByOrderByIdAsc();
        verify(systemSettingsRepo, times(1)).save(any(SystemSettings.class));
    }
}