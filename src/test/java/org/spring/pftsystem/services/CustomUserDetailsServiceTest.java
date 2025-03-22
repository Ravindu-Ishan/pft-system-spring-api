package org.spring.pftsystem.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.pftsystem.entity.schema.main.User;
import org.spring.pftsystem.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {

    private static final String USER_ID = "testUserId";
    private static final String PASSWORD = "hashedPassword";
    private static final String ROLE = "USER";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(USER_ID);
        mockUser.setPassword(PASSWORD);
        mockUser.setRole(ROLE);
    }

    @Test
    void loadUserByUsername_WhenUserExists_ReturnsUserDetails() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mockUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(USER_ID);

        // Assert
        assertNotNull(userDetails);
        assertEquals(USER_ID, userDetails.getUsername());
        assertEquals(PASSWORD, userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + ROLE)));

        verify(userRepository, times(1)).findById(USER_ID);
    }

    @Test
    void loadUserByUsername_WhenUserDoesNotExist_ThrowsUsernameNotFoundException() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername(USER_ID);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userRepository, times(1)).findById(USER_ID);
    }

    @Test
    void loadUserByUsername_WhenRepositoryThrowsException_PropagatesException() {
        // Arrange
        when(userRepository.findById(USER_ID)).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            userDetailsService.loadUserByUsername(USER_ID);
        });

        verify(userRepository, times(1)).findById(USER_ID);
    }


    @Test
    void loadUserByUsername_WhenCalledWithNullId_ThrowsException() {
        // Act & Assert
        assertThrows(Exception.class, () -> {
            userDetailsService.loadUserByUsername(null);
        });
    }
}
