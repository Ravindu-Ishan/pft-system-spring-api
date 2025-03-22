package org.spring.pftsystem.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.pftsystem.entity.schema.main.User;
import org.spring.pftsystem.entity.schema.sub.NotificationPreferences;
import org.spring.pftsystem.entity.schema.sub.UserSettings;
import org.spring.pftsystem.exception.DatabaseOperationException;
import org.spring.pftsystem.repository.UserRepository;
import org.spring.pftsystem.utility.UserUtil;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserSettingsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserSettingsService userSettingsService;

    private User user;
    private UserSettings userSettings;
    private NotificationPreferences notificationPreferences;
    private MockedStatic<UserUtil> userUtilMockedStatic;

    @BeforeEach
    void setUp() {
        // Setup notification preferences
        notificationPreferences = new NotificationPreferences();

        // Setup mock user and settings
        userSettings = new UserSettings();
        userSettings.setCurrency("USD");
        userSettings.setNotificationPreferences(notificationPreferences);

        user = new User();
        user.setId("user123");
        user.setSettings(userSettings);

        // Mock static method in UserUtil
        userUtilMockedStatic = Mockito.mockStatic(UserUtil.class);
        userUtilMockedStatic.when(() -> UserUtil.getUserFromContext(userRepository)).thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        if (userUtilMockedStatic != null) {
            userUtilMockedStatic.close();
        }
    }

    @Test
    void testGetUserSettings_Success() {
        // Act
        UserSettings result = userSettingsService.getUserSettings();

        // Assert
        assertNotNull(result);
        assertEquals("USD", result.getCurrency());
        assertNotNull(result.getNotificationPreferences());

        // Verify
        userUtilMockedStatic.verify(() -> UserUtil.getUserFromContext(userRepository), times(1));
    }

    @Test
    void testUpdateSettings_Success() {
        // Arrange
        NotificationPreferences newNotificationPreferences = new NotificationPreferences();
        // Set any properties of notification preferences if needed

        UserSettings newSettings = new UserSettings();
        newSettings.setCurrency("EUR");
        newSettings.setNotificationPreferences(newNotificationPreferences);

        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        UserSettings result = userSettingsService.updateSettings(newSettings);

        // Assert
        assertNotNull(result);
        assertEquals("EUR", result.getCurrency());
        assertNotNull(result.getNotificationPreferences());

        // Verify
        verify(userRepository, times(1)).save(user);
        userUtilMockedStatic.verify(() -> UserUtil.getUserFromContext(userRepository), times(1));
    }

    @Test
    void testUpdateSettings_DatabaseException() {
        // Arrange
        UserSettings newSettings = new UserSettings();
        newSettings.setCurrency("EUR");
        newSettings.setNotificationPreferences(new NotificationPreferences());

        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        DatabaseOperationException exception = assertThrows(
                DatabaseOperationException.class,
                () -> userSettingsService.updateSettings(newSettings)
        );

        assertEquals("Database connection error", exception.getMessage());

        // Verify
        verify(userRepository, times(1)).save(user);
        userUtilMockedStatic.verify(() -> UserUtil.getUserFromContext(userRepository), times(1));
    }

    @Test
    void testUpdateSettings_NullSettings() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        UserSettings result = userSettingsService.updateSettings(null);

        // Assert
        assertNull(result);

        // Verify
        verify(userRepository, times(1)).save(user);
        userUtilMockedStatic.verify(() -> UserUtil.getUserFromContext(userRepository), times(1));
    }

    @Test
    void testGetUserSettings_WithNullSettings() {
        // Arrange
        User userWithNullSettings = new User();
        userWithNullSettings.setId("user123");
        userWithNullSettings.setSettings(null);

        userUtilMockedStatic.when(() -> UserUtil.getUserFromContext(userRepository)).thenReturn(userWithNullSettings);

        // Act
        UserSettings result = userSettingsService.getUserSettings();

        // Assert
        assertNull(result);

        // Verify
        userUtilMockedStatic.verify(() -> UserUtil.getUserFromContext(userRepository), times(1));
    }

    @Test
    void testDefaultValues() {
        // Arrange
        UserSettings defaultSettings = new UserSettings();

        // Act - no action needed, just testing defaults

        // Assert
        assertEquals("LKR", defaultSettings.getCurrency(), "Default currency should be LKR");
        assertNotNull(defaultSettings.getNotificationPreferences(), "NotificationPreferences should be initialized");
    }
}