package org.spring.pftsystem.services;

import io.lettuce.core.RedisException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.spring.pftsystem.entity.schema.main.User;
import org.spring.pftsystem.repository.UserRepository;
import org.spring.pftsystem.utility.JwtUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.security.auth.login.CredentialNotFoundException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegister() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        String result = authService.register("test@example.com", "password");
        assertEquals("User Registered Successfully", result);
        verify(userRepository, times(1)).save(any(User.class));
    }


    @Test
    void testLoginInvalidCredentials() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        assertThrows(CredentialNotFoundException.class, () -> authService.login("test@example.com", "password"));
    }


    @Test
    void testLogoutInvalidToken() {
        assertThrows(IllegalArgumentException.class, () -> authService.logout("invalid_token"));
    }

    // Mocking ValueOperations for testLoginSuccess
    @Test
    void testLoginSuccess() throws CredentialNotFoundException {
        User user = new User();
        user.setId("1");
        user.setEmail("test@example.com");
        user.setPassword(new BCryptPasswordEncoder().encode("password"));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(anyString(), anyMap())).thenReturn("token");

        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String token = authService.login("test@example.com", "password");
        assertNotNull(token);
        verify(valueOperations, times(1)).set("TOKEN_1", "token", 1, TimeUnit.HOURS);
    }

}
