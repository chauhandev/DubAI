package com.dAdK.dubAI.controller;

import com.dAdK.dubAI.dto.userdto.LoginRequestDTO;
import com.dAdK.dubAI.dto.userdto.OtpVerificationRequestDTO;
import com.dAdK.dubAI.dto.userdto.UserRequestDTO;
import com.dAdK.dubAI.dto.userdto.UserResponseDTO;
import com.dAdK.dubAI.models.User;
import com.dAdK.dubAI.services.authservice.AuthService;
import com.dAdK.dubAI.services.otp.OtpService;
import com.dAdK.dubAI.services.userservice.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User sign-up, login and token generation")
public class AuthController {

    @Autowired
    private UserService userService;
    @Autowired
    private AuthService authService;
    @Autowired
    private OtpService otpService; // Inject OtpService
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Operation(summary = "Initiate user registration with OTP", description = "Validates user input, creates a pending user, and sends an OTP")
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerUser(@Valid @RequestBody UserRequestDTO userRequestDTO) {
        userService.userValidation(userRequestDTO);
        User user = userService.createPendingUser(userRequestDTO); // Create user with pending status
        otpService.generateOtp(user); // Generate and send OTP

        // Return a response indicating OTP has been sent
        // Correctly format LocalDate to String for UserResponseDTO
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "OTP sent to your registered email/phone. Please verify.", "userId", user.getId()));
    }

    @Operation(summary = "Verify OTP and complete registration", description = "Verifies the OTP and completes user registration")
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@RequestBody OtpVerificationRequestDTO otpVerificationRequestDTO) {
        // Fetch the user using the provided userId
        Optional<User> userOptional = otpService.findByUserId(otpVerificationRequestDTO.getUserId());

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (otpService.validateOtp(user, otpVerificationRequestDTO.getOtp())) {
                user.setStatus("ACTIVE");
                userService.saveUser(user);
                return ResponseEntity.ok(Map.of("message", "User registered successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Invalid or expired OTP"));
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User Details not found"));
        }
    }


    @Operation(summary = "User login", description = "Login using username, email or contact number and get JWT token")
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequestDTO dto) {
        String token = authService.login(dto);
        return ResponseEntity.ok(Map.of("token", token));
    }

    @Operation(summary = "Fetch user details", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/details")
    public ResponseEntity<UserResponseDTO> getDetails(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userId = ((User) authentication.getPrincipal()).getUsername();
        return userService.getUser(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
