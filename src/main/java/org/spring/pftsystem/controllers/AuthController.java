package org.spring.pftsystem.controllers;

import org.spring.pftsystem.entity.request.LoginRequest;
import org.spring.pftsystem.entity.request.RegisterRequest;
import org.spring.pftsystem.entity.response.AuthResponse;
import org.spring.pftsystem.entity.response.LogoutResponse;
import org.spring.pftsystem.entity.response.RegistrationResponse;
import org.spring.pftsystem.services.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.CredentialNotFoundException;

@RestController
@RequestMapping("/api/auth")
public class AuthController  {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@RequestBody RegisterRequest request) {
        String response = authService.register(request.getEmail(), request.getPassword());
        RegistrationResponse registrationResponse = new RegistrationResponse(HttpStatus.CREATED.value(),response);
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationResponse);
    }

    @PostMapping("/registerAdmin")
    public ResponseEntity<RegistrationResponse> registerAdmin(@RequestBody RegisterRequest request) {
        String response = authService.registerAdmin(request.getEmail(), request.getPassword());
        RegistrationResponse registrationResponse = new RegistrationResponse(HttpStatus.CREATED.value(),response);
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) throws CredentialNotFoundException {
        String token = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(new AuthResponse("Login Success", token));
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@RequestHeader("Authorization") String token) {
       authService.logout(token);
       return ResponseEntity.ok(new LogoutResponse("Logout Success"));
    }
}