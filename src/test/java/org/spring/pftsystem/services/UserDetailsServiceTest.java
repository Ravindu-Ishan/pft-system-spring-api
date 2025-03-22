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
import org.spring.pftsystem.entity.response.UserDetails;
import org.spring.pftsystem.entity.schema.main.User;
import org.spring.pftsystem.exception.DatabaseOperationException;
import org.spring.pftsystem.exception.UserNotFoundException;
import org.spring.pftsystem.repository.UserRepository;
import org.spring.pftsystem.utility.UserUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsService userDetailsService;

    private User user;
    private UserDetails userDetails;
    private MockedStatic<UserUtil> userUtilMockedStatic;

    @BeforeEach
    void setUp() {
        // Setup mock user
        user = new User();
        user.setId("user123");
        user.setEmail("test@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setRole("User");
        user.setPassword("hashedPassword");

        // Setup mock user details
        userDetails = new UserDetails();
        userDetails.setEmail("test@example.com");
        userDetails.setFirstName("John");
        userDetails.setLastName("Doe");

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
    void testGetUserDetails_Success() {
        // Act
        UserDetails result = userDetailsService.getUserDetails();

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());

        // Verify
        userUtilMockedStatic.verify(() -> UserUtil.getUserFromContext(userRepository), times(1));
    }


    @Test
    void testUpdateUserDetails_NullUserDetails() {
        // Arrange
        UserDetails nullDetails = null;

        // Act & Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> userDetailsService.updateUserDetails(nullDetails)
        );

        // Verify
        verify(userRepository, never()).save(any(User.class));
        userUtilMockedStatic.verify(() -> UserUtil.getUserFromContext(userRepository), times(1));
    }

    @Test
    void testGetUserDetails_UserHasNoDetails() {
        // Arrange
        User emptyUser = new User();
        emptyUser.setId("user123");
        // Not setting email, firstName, lastName

        userUtilMockedStatic.when(() -> UserUtil.getUserFromContext(userRepository)).thenReturn(emptyUser);

        // Act
        UserDetails result = userDetailsService.getUserDetails();

        // Assert
        assertNotNull(result);
        assertNull(result.getEmail());
        assertNull(result.getFirstName());
        assertNull(result.getLastName());

        // Verify
        userUtilMockedStatic.verify(() -> UserUtil.getUserFromContext(userRepository), times(1));
    }

    @Test
    void testUpdateUserDetails_Success() {
        // Arrange
        UserDetails updatedDetails = new UserDetails();
        updatedDetails.setEmail("updated@example.com");
        updatedDetails.setFirstName("Jane");
        updatedDetails.setLastName("Smith");

        // Mock findById
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));

        // Mock findByEmail - should return null since email doesn't exist yet
        when(userRepository.findByEmail("updated@example.com")).thenReturn(Optional.empty());

        when(userRepository.save(any(User.class))).thenReturn(user);

        // Reset mock for UserUtil for the second call in getUserDetails method
        User savedUser = new User();
        savedUser.setId("user123");
        savedUser.setEmail("updated@example.com");
        savedUser.setFirstName("Jane");
        savedUser.setLastName("Smith");
        userUtilMockedStatic.when(() -> UserUtil.getUserFromContext(userRepository))
                .thenReturn(user)  // First call in updateUserDetails
                .thenReturn(savedUser); // Second call in getUserDetails

        // Act
        UserDetails result = userDetailsService.updateUserDetails(updatedDetails);

        // Assert
        assertNotNull(result);
        assertEquals("updated@example.com", result.getEmail());
        assertEquals("Jane", result.getFirstName());
        assertEquals("Smith", result.getLastName());

        // Verify interactions
        verify(userRepository, times(1)).findById("user123");
        verify(userRepository, times(1)).findByEmail("updated@example.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testUpdateUserDetails_EmailAlreadyExists() {
        // Arrange
        UserDetails updatedDetails = new UserDetails();
        updatedDetails.setEmail("existing@example.com");
        updatedDetails.setFirstName("Jane");
        updatedDetails.setLastName("Smith");

        // Original user
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));

        // Simulate existing user with this email
        User existingUser = new User();
        existingUser.setId("user456");
        existingUser.setEmail("existing@example.com");
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userDetailsService.updateUserDetails(updatedDetails)
        );

        assertEquals("Email already exists", exception.getMessage());

        // Verify
        verify(userRepository, times(1)).findById("user123");
        verify(userRepository, times(1)).findByEmail("existing@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateUserDetails_EmptyEmail() {
        // Arrange
        UserDetails updatedDetails = new UserDetails();
        updatedDetails.setEmail(""); // Empty email
        updatedDetails.setFirstName("Jane");
        updatedDetails.setLastName("Smith");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userDetailsService.updateUserDetails(updatedDetails)
        );

        assertEquals("Email cannot be empty", exception.getMessage());

        // Verify
        verify(userRepository, never()).findById(anyString());
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateUserDetails_UserNotFound() {
        // Arrange
        UserDetails updatedDetails = new UserDetails();
        updatedDetails.setEmail("updated@example.com");

        // Simulate user not found
        when(userRepository.findById("user123")).thenReturn(Optional.empty());

        // Act & Assert
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userDetailsService.updateUserDetails(updatedDetails)
        );

        // Verify
        verify(userRepository, times(1)).findById("user123");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateUserDetails_NoChangedEmail() {
        // Arrange
        UserDetails updatedDetails = new UserDetails();
        updatedDetails.setEmail("test@example.com"); // Same as original
        updatedDetails.setFirstName("Jane");
        updatedDetails.setLastName("Smith");

        // Mock findById
        when(userRepository.findById("user123")).thenReturn(Optional.of(user));

        when(userRepository.save(any(User.class))).thenReturn(user);

        // Reset mock for UserUtil for the second call in getUserDetails method
        User savedUser = new User();
        savedUser.setId("user123");
        savedUser.setEmail("test@example.com");
        savedUser.setFirstName("Jane");
        savedUser.setLastName("Smith");
        userUtilMockedStatic.when(() -> UserUtil.getUserFromContext(userRepository))
                .thenReturn(user)  // First call in updateUserDetails
                .thenReturn(savedUser); // Second call in getUserDetails

        // Act
        UserDetails result = userDetailsService.updateUserDetails(updatedDetails);

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals("Jane", result.getFirstName());
        assertEquals("Smith", result.getLastName());

        // Verify
        verify(userRepository, times(1)).findById("user123");
        verify(userRepository, never()).findByEmail(anyString()); // Should not check for email since it didn't change
        verify(userRepository, times(1)).save(any(User.class));
    }
}