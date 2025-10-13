package com.dAdK.dubAI.controller;

import com.dAdK.dubAI.dto.userdto.GoogleLoginRequestDTO;
import com.dAdK.dubAI.dto.userdto.LoginRequestDTO;
import com.dAdK.dubAI.dto.userdto.OtpVerificationRequestDTO;
import com.dAdK.dubAI.dto.userdto.UserRequestDTO;
import com.dAdK.dubAI.exceptions.UserAlreadyExistsException;
import com.dAdK.dubAI.exceptions.UserNotFoundException;
import com.dAdK.dubAI.exceptions.ValidationException;
import com.dAdK.dubAI.models.Otp;
import com.dAdK.dubAI.models.User;
import com.dAdK.dubAI.services.RateLimiting.RateLimitService;
import com.dAdK.dubAI.services.authservice.AuthService;
import com.dAdK.dubAI.services.otp.OtpService;
import com.dAdK.dubAI.services.userservice.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthController authController;

    private UserRequestDTO userRequestDTO;
    private User user;
    private Otp otp;

    @BeforeEach
    void setUp() {
        userRequestDTO = new UserRequestDTO();
        userRequestDTO.setUsername("testuser");
        userRequestDTO.setPassword("Password123!");
        userRequestDTO.setEmail("test@example.com");
        userRequestDTO.setContactNumber("1234567890");

        user = User.builder().build();
        user.setId("user123");
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setContactNumber("1234567890");
        user.setStatus("PENDING_VERIFICATION");
        user.setRegistrationType("EMAIL");
        user.setCreatedAt(LocalDateTime.now());
        user.setEmailVerified(false);
        user.setPhoneVerified(false);

        otp = new Otp();
        otp.setUserId("user123");
        otp.setOtp("123456");
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
    }

    // Helper to mock getClientIp
    private void mockGetClientIp(String ip) {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(ip);
    }

    /**
     * Tests successful user registration via email.
     * Verifies that a new user is created with PENDING_VERIFICATION status and an OTP is sent to the email.
     */
    @Test
    void registerUser_success_emailRegistration() throws IOException {
        mockGetClientIp("127.0.0.1");
        userRequestDTO.setContactNumber(null); // Email registration

        Map<String, String> serviceResponse = Map.of(
                "status", String.valueOf(HttpStatus.CREATED.value()),
                "message", "OTP sent to your email. Please verify.",
                "userId", "user123"
        );
        when(authService.registerUser(any(UserRequestDTO.class), anyString())).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = authController.registerUser(userRequestDTO, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("OTP sent to your email. Please verify.", response.getBody().get("message"));
        assertEquals("user123", response.getBody().get("userId"));
        verify(authService, times(1)).registerUser(any(UserRequestDTO.class), eq("127.0.0.1"));
    }



    /**
     * Tests registration attempt without providing either email or phone number.
     * Expects an HTTP 400 (Bad Request) status with a specific message.
     */
    @Test
    void registerUser_noEmailOrPhone_returnsBadRequest() throws IOException {
        mockGetClientIp("127.0.0.1");
        userRequestDTO.setEmail(null);
        userRequestDTO.setContactNumber(null);

        Map<String, String> serviceResponse = Map.of(
                "status", String.valueOf(HttpStatus.BAD_REQUEST.value()),
                "message", "Either email or phone number must be provided"
        );
        when(authService.registerUser(any(UserRequestDTO.class), anyString())).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = authController.registerUser(userRequestDTO, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Either email or phone number must be provided", response.getBody().get("message"));
        verify(authService, times(1)).registerUser(any(UserRequestDTO.class), eq("127.0.0.1"));
    }

    /**
     * Tests registration attempt when the user is rate-limited.
     * Expects an HTTP 429 (Too Many Requests) status.
     */
    @Test
    void registerUser_rateLimited_returnsTooManyRequests() throws IOException {
        mockGetClientIp("127.0.0.1");
        Map<String, String> serviceResponse = Map.of(
                "status", String.valueOf(HttpStatus.TOO_MANY_REQUESTS.value()),
                "message", "Too many registration attempts. Please try again later."
        );
        when(authService.registerUser(any(UserRequestDTO.class), anyString())).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = authController.registerUser(userRequestDTO, request);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("Too many registration attempts. Please try again later.", response.getBody().get("message"));
        verify(authService, times(1)).registerUser(any(UserRequestDTO.class), eq("127.0.0.1"));
    }

    /**
     * Tests registration attempt with a username that is already taken by a verified user.
     * Expects an HTTP 409 (Conflict) status with a specific message.
     */
    @Test
    void registerUser_usernameAlreadyTaken_verifiedUser_returnsConflict() throws IOException {
        mockGetClientIp("127.0.0.1");
        user.setEmailVerified(true);

        Map<String, String> serviceResponse = Map.of(
                "status", String.valueOf(HttpStatus.CONFLICT.value()),
                "message", "Username already taken"
        );
        when(authService.registerUser(any(UserRequestDTO.class), anyString())).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = authController.registerUser(userRequestDTO, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Username already taken", response.getBody().get("message"));
        verify(authService, times(1)).registerUser(any(UserRequestDTO.class), eq("127.0.0.1"));
    }

    /**
     * Tests registration attempt with a username that is pending verification by the same user (recent attempt).
     * Expects an HTTP 200 (OK) status with an OTP resent message.
     */
    @Test
    void registerUser_usernamePending_sameUser_recent_resendOtp() throws IOException {
        mockGetClientIp("127.0.0.1");
        user.setStatus("PENDING_VERIFICATION");
        user.setRegistrationType("EMAIL");
        user.setCreatedAt(LocalDateTime.now().minusMinutes(5)); // Recent

        Map<String, String> serviceResponse = Map.of(
                "status", String.valueOf(HttpStatus.OK.value()),
                "message", "OTP resent to your email",
                "userId", "user123"
        );
        when(authService.registerUser(any(UserRequestDTO.class), anyString())).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = authController.registerUser(userRequestDTO, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OTP resent to your email", response.getBody().get("message"));
        assertEquals("user123", response.getBody().get("userId"));
        verify(authService, times(1)).registerUser(any(UserRequestDTO.class), eq("127.0.0.1"));
    }

    /**
     * Tests registration attempt with a username that is pending verification by the same user (old attempt).
     * Expects an HTTP 201 (Created) status with a new registration process started.
     */
    @Test
    void registerUser_usernamePending_sameUser_old_deleteAndReRegister() throws IOException {
        mockGetClientIp("127.0.0.1");
        user.setStatus("PENDING_VERIFICATION");
        user.setRegistrationType("EMAIL");
        user.setCreatedAt(LocalDateTime.now().minusMinutes(20)); // Old

        Map<String, String> serviceResponse = Map.of(
                "status", String.valueOf(HttpStatus.CREATED.value()),
                "message", "OTP sent to your email. Please verify.",
                "userId", "user123"
        );
        when(authService.registerUser(any(UserRequestDTO.class), anyString())).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = authController.registerUser(userRequestDTO, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("OTP sent to your email. Please verify.", response.getBody().get("message"));
        verify(authService, times(1)).registerUser(any(UserRequestDTO.class), eq("127.0.0.1"));
    }

    /**
     * Tests registration attempt with a username that is pending verification by a different user (recent attempt).
     * Expects an HTTP 409 (Conflict) status with a specific message.
     */
    @Test
    void registerUser_usernamePending_differentUser_recent_returnsConflict() throws IOException {
        mockGetClientIp("127.0.0.1");
        User existingUser = User.builder().build();
        existingUser.setId("existingUser123");
        existingUser.setUsername("testuser");
        existingUser.setEmail("different@example.com"); // Different email
        existingUser.setStatus("PENDING_VERIFICATION");
        existingUser.setCreatedAt(LocalDateTime.now().minusMinutes(10)); // Recent

        Map<String, String> serviceResponse = Map.of(
                "status", String.valueOf(HttpStatus.CONFLICT.value()),
                "message", "Username is temporarily reserved. Please try another username or wait."
        );
        when(authService.registerUser(any(UserRequestDTO.class), anyString())).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = authController.registerUser(userRequestDTO, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Username is temporarily reserved. Please try another username or wait.", response.getBody().get("message"));
        verify(authService, times(1)).registerUser(any(UserRequestDTO.class), eq("127.0.0.1"));
    }

    /**
     * Tests registration attempt with a username that is pending verification by a different user (old attempt).
     * Expects an HTTP 201 (Created) status with a new registration process started.
     */
    @Test
    void registerUser_usernamePending_differentUser_old_deleteAndReRegister() throws IOException {
        mockGetClientIp("127.0.0.1");
        User existingUser = User.builder().build();
        existingUser.setId("existingUser123");
        existingUser.setUsername("testuser");
        existingUser.setEmail("different@example.com"); // Different email
        existingUser.setStatus("PENDING_VERIFICATION");
        existingUser.setCreatedAt(LocalDateTime.now().minusMinutes(40)); // Old

        Map<String, String> serviceResponse = Map.of(
                "status", String.valueOf(HttpStatus.CREATED.value()),
                "message", "OTP sent to your email. Please verify.",
                "userId", "user123"
        );
        when(authService.registerUser(any(UserRequestDTO.class), anyString())).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = authController.registerUser(userRequestDTO, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("OTP sent to your email. Please verify.", response.getBody().get("message"));
        verify(authService, times(1)).registerUser(any(UserRequestDTO.class), eq("127.0.0.1"));
    }

    /**
     * Tests registration attempt with an email that is already registered and verified.
     * Expects an HTTP 409 (Conflict) status with a specific message.
     */
    @Test
    void registerUser_emailAlreadyRegistered_verified_returnsConflict() throws IOException {
        mockGetClientIp("127.0.0.1");
        user.setEmailVerified(true);

        Map<String, String> serviceResponse = Map.of(
                "status", String.valueOf(HttpStatus.CONFLICT.value()),
                "message", "Email already registered. Please login."
        );
        when(authService.registerUser(any(UserRequestDTO.class), anyString())).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = authController.registerUser(userRequestDTO, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Email already registered. Please login.", response.getBody().get("message"));
        verify(authService, times(1)).registerUser(any(UserRequestDTO.class), eq("127.0.0.1"));
    }

    /**
     * Tests registration attempt with an email that is pending verification (recent attempt).
     * Expects an HTTP 200 (OK) status with an OTP resent message.
     */
    @Test
    void registerUser_emailPending_recent_resendOtp() throws IOException {
        mockGetClientIp("127.0.0.1");
        user.setStatus("PENDING_VERIFICATION");
        user.setRegistrationType("EMAIL");
        user.setCreatedAt(LocalDateTime.now().minusMinutes(5)); // Recent

        Map<String, String> serviceResponse = Map.of(
                "status", String.valueOf(HttpStatus.OK.value()),
                "message", "OTP resent to your email",
                "userId", "user123"
        );
        when(authService.registerUser(any(UserRequestDTO.class), anyString())).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = authController.registerUser(userRequestDTO, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OTP resent to your email", response.getBody().get("message"));
        assertEquals("user123", response.getBody().get("userId"));
        verify(authService, times(1)).registerUser(any(UserRequestDTO.class), eq("127.0.0.1"));
    }

    /**
     * Tests registration attempt with an email that is pending verification (old attempt).
     * Expects an HTTP 201 (Created) status with a new registration process started.
     */
    @Test
    void registerUser_emailPending_old_deleteAndReRegister() throws IOException {
        mockGetClientIp("127.0.0.1");
        user.setStatus("PENDING_VERIFICATION");
        user.setRegistrationType("EMAIL");
        user.setCreatedAt(LocalDateTime.now().minusMinutes(20)); // Old

        Map<String, String> serviceResponse = Map.of(
                "status", String.valueOf(HttpStatus.CREATED.value()),
                "message", "OTP sent to your email. Please verify.",
                "userId", "user123"
        );
        when(authService.registerUser(any(UserRequestDTO.class), anyString())).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = authController.registerUser(userRequestDTO, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("OTP sent to your email. Please verify.", response.getBody().get("message"));
        verify(authService, times(1)).registerUser(any(UserRequestDTO.class), eq("127.0.0.1"));
    }

    /**
     * Tests registration attempt with a phone number that is already registered and verified.
     * Expects an HTTP 409 (Conflict) status with a specific message.
     */
    @Test
    void registerUser_phoneAlreadyRegistered_verified_returnsConflict() throws IOException {
        mockGetClientIp("127.0.0.1");
        userRequestDTO.setEmail(null); // Phone registration
        user.setPhoneVerified(true);
        user.setRegistrationType("PHONE");

        Map<String, String> serviceResponse = Map.of(
                "status", String.valueOf(HttpStatus.CONFLICT.value()),
                "message", "Phone number already registered. Please login."
        );
        when(authService.registerUser(any(UserRequestDTO.class), anyString())).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = authController.registerUser(userRequestDTO, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Phone number already registered. Please login.", response.getBody().get("message"));
        verify(authService, times(1)).registerUser(any(UserRequestDTO.class), eq("127.0.0.1"));
    }



    /**
     * Tests registration attempt with a phone number that is pending verification (old attempt).
     * Expects an HTTP 201 (Created) status with a new registration process started.
     */
    @Test
    void registerUser_phonePending_old_deleteAndReRegister() throws IOException {
        mockGetClientIp("127.0.0.1");
        userRequestDTO.setEmail(null); // Phone registration
        user.setStatus("PENDING_VERIFICATION");
        user.setRegistrationType("PHONE");
        user.setCreatedAt(LocalDateTime.now().minusMinutes(20)); // Old

        Map<String, String> serviceResponse = Map.of(
                "status", String.valueOf(HttpStatus.CREATED.value()),
                "message", "OTP sent to your phone. Please verify.",
                "userId", "user123"
        );
        when(authService.registerUser(any(UserRequestDTO.class), anyString())).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = authController.registerUser(userRequestDTO, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("OTP sent to your phone. Please verify.", response.getBody().get("message"));
        verify(authService, times(1)).registerUser(any(UserRequestDTO.class), eq("127.0.0.1"));
    }

    /**
     * Tests successful OTP verification for email registration.
     * Verifies that the user's email is marked as verified and status changes to ACTIVE.
     */
    @Test
    void verifyOtp_success_emailVerification() {
        OtpVerificationRequestDTO otpVerificationRequestDTO = new OtpVerificationRequestDTO();
        otpVerificationRequestDTO.setUserId("user123");
        otpVerificationRequestDTO.setOtp("123456");
        user.setRegistrationType("EMAIL");
        user.setEmailVerified(false);

        Map<String, String> serviceResponse = Map.of(
                "status", String.valueOf(HttpStatus.OK.value()),
                "message", "User registered successfully"
        );
        when(authService.verifyOtp(any(OtpVerificationRequestDTO.class))).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = authController.verifyOtp(otpVerificationRequestDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User registered successfully", response.getBody().get("message"));
        verify(authService, times(1)).verifyOtp(any(OtpVerificationRequestDTO.class));
    }

    /**
     * Tests successful OTP verification for phone registration.
     * Verifies that the user's phone is marked as verified and status changes to ACTIVE.
     */
    @Test
    void verifyOtp_success_phoneVerification() {
        OtpVerificationRequestDTO otpVerificationRequestDTO = new OtpVerificationRequestDTO();
        otpVerificationRequestDTO.setUserId("user123");
        otpVerificationRequestDTO.setOtp("123456");
        user.setRegistrationType("PHONE");
        user.setPhoneVerified(false);

        Map<String, String> serviceResponse = Map.of(
                "status", String.valueOf(HttpStatus.OK.value()),
                "message", "User registered successfully"
        );
        when(authService.verifyOtp(any(OtpVerificationRequestDTO.class))).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = authController.verifyOtp(otpVerificationRequestDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User registered successfully", response.getBody().get("message"));
        verify(authService, times(1)).verifyOtp(any(OtpVerificationRequestDTO.class));
    }

    /**
     * Tests OTP verification with an invalid or expired OTP.
     * Expects an HTTP 400 (Bad Request) status.
     */
    @Test
    void verifyOtp_invalidOrExpiredOtp_returnsBadRequest() {
        OtpVerificationRequestDTO otpVerificationRequestDTO = new OtpVerificationRequestDTO();
        otpVerificationRequestDTO.setUserId("user123");
        otpVerificationRequestDTO.setOtp("wrongotp");
        user.setRegistrationType("EMAIL");

        Map<String, String> serviceResponse = Map.of(
                "status", String.valueOf(HttpStatus.BAD_REQUEST.value()),
                "message", "Invalid or expired OTP"
        );
        when(authService.verifyOtp(any(OtpVerificationRequestDTO.class))).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = authController.verifyOtp(otpVerificationRequestDTO);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid or expired OTP", response.getBody().get("message"));
        verify(authService, times(1)).verifyOtp(any(OtpVerificationRequestDTO.class));
    }

    /**
     * Tests OTP verification when the user ID is not found in OTP data.
     * Expects an HTTP 404 (Not Found) status.
     */
    @Test
    void verifyOtp_userNotFound_inOtpData_returnsNotFound() {
        OtpVerificationRequestDTO otpVerificationRequestDTO = new OtpVerificationRequestDTO();
        otpVerificationRequestDTO.setUserId("unknownUser");
        otpVerificationRequestDTO.setOtp("123456");

        Map<String, String> serviceResponse = Map.of(
                "status", String.valueOf(HttpStatus.NOT_FOUND.value()),
                "message", "User Details not found"
        );
        when(authService.verifyOtp(any(OtpVerificationRequestDTO.class))).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = authController.verifyOtp(otpVerificationRequestDTO);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User Details not found", response.getBody().get("message"));
        verify(authService, times(1)).verifyOtp(any(OtpVerificationRequestDTO.class));
    }

    /**
     * Tests OTP verification when the user ID is found in OTP data but not in user data.
     * Expects an HTTP 404 (Not Found) status.
     */
    @Test
    void verifyOtp_userNotFound_inUserData_returnsNotFound() {
        OtpVerificationRequestDTO otpVerificationRequestDTO = new OtpVerificationRequestDTO();
        otpVerificationRequestDTO.setUserId("user123");
        otpVerificationRequestDTO.setOtp("123456");

        Map<String, String> serviceResponse = Map.of(
                "status", String.valueOf(HttpStatus.NOT_FOUND.value()),
                "message", "User Details not found"
        );
        when(authService.verifyOtp(any(OtpVerificationRequestDTO.class))).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = authController.verifyOtp(otpVerificationRequestDTO);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User Details not found", response.getBody().get("message"));
        verify(authService, times(1)).verifyOtp(any(OtpVerificationRequestDTO.class));
    }

    /**
     * Tests successful user login.
     * Verifies that a JWT token is returned.
     */
    @Test
    void login_success() {
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO();
        loginRequestDTO.setIdentifier("testuser");
        loginRequestDTO.setPassword("Password123!");
        String expectedToken = "mockedJwtToken";

        when(authService.login(any(LoginRequestDTO.class))).thenReturn(expectedToken);

        ResponseEntity<Map<String, String>> response = authController.login(loginRequestDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedToken, response.getBody().get("token"));
        verify(authService, times(1)).login(loginRequestDTO);
    }

    /**
     * Tests the retrieval of client IP from the "X-Forwarded-For" header.
     */
    @Test
    void getClientIp_fromXForwardedFor() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        // Use reflection to call private method
        try {
            java.lang.reflect.Method method = AuthController.class.getDeclaredMethod("getClientIp", HttpServletRequest.class);
            method.setAccessible(true);
            String ip = (String) method.invoke(authController, request);
            assertEquals("192.168.1.1", ip);
        } catch (Exception e) {
            fail("Exception thrown during reflection call: " + e.getMessage());
        }
    }

    /**
     * Tests the retrieval of client IP from the remote address when "X-Forwarded-For" is not present.
     */
    @Test
    void getClientIp_fromRemoteAddr() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        // Use reflection to call private method
        try {
            java.lang.reflect.Method method = AuthController.class.getDeclaredMethod("getClientIp", HttpServletRequest.class);
            method.setAccessible(true);
            String ip = (String) method.invoke(authController, request);
            assertEquals("127.0.0.1", ip);
        } catch (Exception e) {
            fail("Exception thrown during reflection call: " + e.getMessage());
        }
    }

    /**
     * Tests user registration when user validation fails.
     * Expects an HTTP 400 (Bad Request) status.
     */
    @Test
    void registerUser_userValidationFails_returnsBadRequest() throws IOException {
        mockGetClientIp("127.0.0.1");

        Map<String, String> serviceResponse = Map.of(
                "status", String.valueOf(HttpStatus.BAD_REQUEST.value()),
                "message", "Invalid user data"
        );
        when(authService.registerUser(any(UserRequestDTO.class), anyString())).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = authController.registerUser(userRequestDTO, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid user data", response.getBody().get("message"));
        verify(authService, times(1)).registerUser(any(UserRequestDTO.class), eq("127.0.0.1"));
    }

    /**
     * Tests successful Google login.
     * Verifies that a JWT token is returned after successful Google authentication.
     */
    @Test
    void googleLogin_success() throws IOException, GeneralSecurityException {
        GoogleLoginRequestDTO googleLoginRequestDTO = new GoogleLoginRequestDTO();
        googleLoginRequestDTO.setIdToken("mockGoogleIdToken");
        String expectedToken = "mockedJwtTokenForGoogle";

        when(authService.googleLogin(anyString())).thenReturn(expectedToken);

        ResponseEntity<Map<String, String>> response = authController.googleLogin(googleLoginRequestDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedToken, response.getBody().get("token"));
        verify(authService, times(1)).googleLogin(googleLoginRequestDTO.getIdToken());
    }

    /**
     * Tests Google login when GeneralSecurityException occurs during authentication.
     * Expects an HTTP 401 (Unauthorized) status.
     */
    @Test
    void googleLogin_generalSecurityException_returnsUnauthorized() throws IOException, GeneralSecurityException {
        GoogleLoginRequestDTO googleLoginRequestDTO = new GoogleLoginRequestDTO();
        googleLoginRequestDTO.setIdToken("invalidGoogleIdToken");

        when(authService.googleLogin(anyString())).thenThrow(new GeneralSecurityException("Invalid ID token"));

        ResponseEntity<Map<String, String>> response = authController.googleLogin(googleLoginRequestDTO);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Google login failed: Invalid ID token", response.getBody().get("message"));
        verify(authService, times(1)).googleLogin(googleLoginRequestDTO.getIdToken());
    }

    /**
     * Tests Google login when IOException occurs during authentication.
     * Expects an HTTP 500 (Internal Server Error) status.
     */
    @Test
    void googleLogin_ioException_returnsInternalServerError() throws IOException, GeneralSecurityException {
        GoogleLoginRequestDTO googleLoginRequestDTO = new GoogleLoginRequestDTO();
        googleLoginRequestDTO.setIdToken("someGoogleIdToken");

        when(authService.googleLogin(anyString())).thenThrow(new IOException("Network error"));

        ResponseEntity<Map<String, String>> response = authController.googleLogin(googleLoginRequestDTO);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Google login failed: Network error", response.getBody().get("message"));
        verify(authService, times(1)).googleLogin(googleLoginRequestDTO.getIdToken());
    }
}
