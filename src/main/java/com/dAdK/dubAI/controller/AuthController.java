package com.dAdK.dubAI.controller;

import com.dAdK.dubAI.dto.userdto.GoogleLoginRequestDTO;
import com.dAdK.dubAI.dto.userdto.LoginRequestDTO;
import com.dAdK.dubAI.dto.userdto.OtpVerificationRequestDTO;
import com.dAdK.dubAI.dto.userdto.UserRequestDTO;
import com.dAdK.dubAI.services.authservice.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;


@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User sign-up, login and token generation")
public class AuthController {

    @Autowired
    private AuthService authService;

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Operation(summary = "Initiate user registration with OTP", description = "Validates user input, creates a pending user, and sends an OTP")
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerUser(@Valid @RequestBody UserRequestDTO userRequestDTO, HttpServletRequest request) throws IOException {
        String ipAddress = getClientIp(request);
        Map<String, String> response = authService.registerUser(userRequestDTO, ipAddress);
        return ResponseEntity.status(Integer.parseInt(response.get("status"))).body(response);
    }

    @Operation(summary = "Verify OTP and complete registration", description = "Verifies the OTP and completes user registration")
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@RequestBody OtpVerificationRequestDTO otpVerificationRequestDTO) {
        Map<String, String> response = authService.verifyOtp(otpVerificationRequestDTO);
        return ResponseEntity.status(Integer.parseInt(response.get("status"))).body(response);
    }

    @Operation(summary = "User login", description = "Login using username, email or contact number and get JWT token")
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequestDTO dto) {
        String token = authService.login(dto);
        return ResponseEntity.ok(Map.of("token", token));
    }

    @Operation(summary = "Sign in with Google", description = "Authenticates user with Google ID token and returns JWT token")
    @PostMapping("/google-login")
    public ResponseEntity<Map<String, String>> googleLogin(@Valid @RequestBody GoogleLoginRequestDTO dto) {
        try {
            String token = authService.googleLogin(dto.getIdToken());
            return ResponseEntity.ok(Map.of("token", token));
        } catch (GeneralSecurityException e) {
            logger.error("Google login failed due to security exception: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Google login failed: " + e.getMessage()));
        } catch (IOException e) {
            logger.error("Google login failed due to IO exception: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Google login failed: " + e.getMessage()));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

}
