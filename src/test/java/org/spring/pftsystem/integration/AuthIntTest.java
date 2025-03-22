package org.spring.pftsystem.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spring.pftsystem.entity.request.LoginRequest;
import org.spring.pftsystem.entity.request.RegisterRequest;
import org.spring.pftsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.data.mongodb.uri=mongodb+srv://admin:admin123@cluster0.cgkcz.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0",
        "spring.data.mongodb.database=pft-system",
        "server.port=9090",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.cache.type=REDIS",
        "spring.jwt.secret=a7690505e17048e02bde918d0a54f00474f47a1428edf724fc39a936459321c8bd8628221900fa01dbb9f166c58b8fcc179233180c252a28eb24a868a63fc85d9931a5241941caf1b70163047e038a6fd945ac16af12bf69896ae6354b28ebe15070b47fe9f589aad6f7f42b844d9d9d0a09629643f4d34f01f147476251c2e149130a4c42173a2c9bf3550fb2d5d58c859a0ede644f28ec953573b0918f4ee08f98bc2597cbb0c6e6d5f82c893bcca1e77d5f1a5bd7163e70a26758a390bd5a71c052289a2aa2f14ec2c257e74c0dd7160806c44221d7d628a39a33babafd9aad5e61a2cc3239d33f24eb47a45ab23eb6e5f4a4ff6149cbff5e9db30de336f2",
        "currency.exchange.api.url=https://api.fastforex.io/convert",
        "currency.exchange.api.key=a4aa70ad64-f410c44df1-ssws3y"
})
public class AuthIntTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Setup - create test user credentials
    private final String TEST_USER_EMAIL = "testuser@example.com";
    private final String TEST_ADMIN_EMAIL = "testadmin@example.com";
    private final String TEST_PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        // Clean database before each test to ensure clean state
        userRepository.deleteAll();
    }

    @Test
    void testRegister() throws Exception {
        // Create request data
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(TEST_USER_EMAIL);
        registerRequest.setPassword(TEST_PASSWORD);

        String requestJson = objectMapper.writeValueAsString(registerRequest);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("User Registered Successfully"));
    }

    @Test
    void testRegisterAdmin() throws Exception {
        // Create request data
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(TEST_ADMIN_EMAIL);
        registerRequest.setPassword(TEST_PASSWORD);

        String requestJson = objectMapper.writeValueAsString(registerRequest);

        mockMvc.perform(post("/api/auth/registerAdmin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(HttpStatus.CREATED.value()))
                .andExpect(jsonPath("$.message").value("User Registered Successfully"));
    }

    @Test
    void testLogin() throws Exception {
        // First register a user so we can log in
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(TEST_USER_EMAIL);
        registerRequest.setPassword(TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Now attempt to login with the registered user
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(TEST_USER_EMAIL);
        loginRequest.setPassword(TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login Success"))
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void testLogout() throws Exception {
        // First register and login to get a valid token
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(TEST_USER_EMAIL);
        registerRequest.setPassword(TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Login to get token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(TEST_USER_EMAIL);
        loginRequest.setPassword(TEST_PASSWORD);

        String responseContent = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Extract token from response
        String token = objectMapper.readTree(responseContent).get("token").asText();

        // Now test logout with the valid token
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout Success"));
    }
}