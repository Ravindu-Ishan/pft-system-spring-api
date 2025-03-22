package org.spring.pftsystem.services;

import io.lettuce.core.RedisException;
import lombok.extern.java.Log;
import org.spring.pftsystem.constants.Constants;
import org.spring.pftsystem.entity.schema.main.User;
import org.spring.pftsystem.repository.UserRepository;
import org.spring.pftsystem.utility.JwtUtil;
import org.spring.pftsystem.utility.UserUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.security.auth.login.CredentialNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Log
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;
    //StringRedisTemplate redisTemplate = new StringRedisTemplate();

    //constructor
    public AuthService(UserRepository userRepository, StringRedisTemplate redisTemplate, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
    }


    public String register(String email, String password) {
        return createUser(email, password, "user");
    }

    public String registerAdmin(String email, String password) {
        return createUser(email, password , "administrator");
    }

    public String login(String email, String password) throws CredentialNotFoundException, IllegalArgumentException {
        // Validate input
        if (email == null || password == null || email.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("Email and password must not be empty");
        }

        User user = userRepository.findByEmail(email).orElseThrow(() -> new CredentialNotFoundException("Invalid credentials"));

        // Password Validation
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.info("Invalid Credentials");
            throw new CredentialNotFoundException("Invalid credentials");
        }

        log.info("Credentials found, Generating token...");
        String token = generateJwtToken(user);


        log.info("Caching Token...");
        try{
            redisTemplate.opsForValue().set("TOKEN_"+ user.getId() , token, 1, TimeUnit.HOURS);
        }catch(Exception e){
            throw new RedisException(e.getMessage());
        }
        log.info("User Logged in successfully");
        return token;
    }

    public boolean logout(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Token Null or Invalid");
        }
        String jwtToken = token.substring(7);
        try{
            User userFromContext = UserUtil.getUserFromContext(userRepository);
            String id = userFromContext.getId();//get id from userContext
            redisTemplate.delete("TOKEN_" + id); //delete cached token
            SecurityContextHolder.clearContext(); // clear security context (for good measures)
            log.info("User logged out successfully");
            return true;
        }catch(Exception e){
            throw new RedisException(e.getMessage());
        }
    }

    private String generateJwtToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        //claims.put("email", user.getEmail());
        claims.put("role", user.getRole());
        return jwtUtil.generateToken(user.getId(), claims);
    }

    private String createUser(String email, String password, String type)
    {
        // Validate input
        if (email == null || password == null || email.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("Email and password must not be empty");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException(Constants.USER_ALREADY_EXISTS);
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password)); // Hash password
        user.setRole(type);
        userRepository.save(user);

        if (type.equalsIgnoreCase("admin")) {
            return "Admin Successfully Registered";
        }
        else {
            return "User Registered Successfully";
        }
    }
}
