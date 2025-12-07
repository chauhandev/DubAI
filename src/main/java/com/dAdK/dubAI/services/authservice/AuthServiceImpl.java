package com.dAdK.dubAI.services.authservice;

import com.dAdK.dubAI.dto.userdto.LoginRequestDTO;
import com.dAdK.dubAI.dto.userdto.OtpVerificationRequestDTO;
import com.dAdK.dubAI.dto.userdto.UserRequestDTO;
import com.dAdK.dubAI.exceptions.InvalidInputException;
import com.dAdK.dubAI.exceptions.UserAlreadyExistsException;
import com.dAdK.dubAI.exceptions.UserNotFoundException;
import com.dAdK.dubAI.models.Otp;
import com.dAdK.dubAI.models.User;
import com.dAdK.dubAI.repository.UserRepository;
import com.dAdK.dubAI.services.RateLimiting.RateLimitService;
import com.dAdK.dubAI.services.otp.OtpService;
import com.dAdK.dubAI.services.userservice.UserService;
import com.dAdK.dubAI.util.JwtService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;
    private final OtpService otpService;
    private final RateLimitService rateLimitService;

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Value("${app.google.client-id}")
    private String googleClientId;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                           UserService userService, OtpService otpService, RateLimitService rateLimitService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userService = userService;
        this.otpService = otpService;
        this.rateLimitService = rateLimitService;
    }

    @Override
    public String login(LoginRequestDTO loginRequestDTO) {
        String identifier = loginRequestDTO.getIdentifier();
        User user = userRepository.findByUsernameOrEmailOrContactNumber(identifier, identifier, identifier).orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return jwtService.generateToken(user);
    }

    @Override
    public Map<String, String> registerUser(UserRequestDTO userRequestDTO, String ipAddress) {
        // 1. Validate that at least email OR phone is provided
        if (!userRequestDTO.hasEmailOrPhone()) {
            throw new InvalidInputException("Either email or phone number must be provided");
        }

        // 2. IP Rate limiting (5 attempts per hour)
        if (rateLimitService.isRateLimited(ipAddress, "register", 5, 1800)) {
            return Map.of("status", String.valueOf(HttpStatus.TOO_MANY_REQUESTS.value()),
                    "message", "Too many registration attempts. Please try again later.");
        }

        // 3. Validate user input (check username/email format, password strength, etc.)
        userService.userValidation(userRequestDTO);

        // 4. Check username uniqueness FIRST (regardless of verification status)
        // This prevents someone from blocking a username with unverified account
        Optional<User> userByUsername = userService.findByUsername(userRequestDTO.getUsername());

        if (userByUsername.isPresent()) {
            User existingUser = userByUsername.get();

            // If username belongs to VERIFIED user, reject immediately
            if (existingUser.getEmailVerified() || existingUser.getPhoneVerified()) {
                throw new UserAlreadyExistsException("Username already taken");
            }

            // If username belongs to UNVERIFIED user (pending)
            if ("PENDING_VERIFICATION".equals(existingUser.getStatus())) {

                // Check if it's the SAME user trying to register again (same email/phone)
                boolean isSameUser = false;

                if (userRequestDTO.getEmail() != null &&
                        userRequestDTO.getEmail().equals(existingUser.getEmail())) {
                    isSameUser = true;
                }

                if (userRequestDTO.getContactNumber() != null &&
                        userRequestDTO.getContactNumber().equals(existingUser.getContactNumber())) {
                    isSameUser = true;
                }

                if (isSameUser) {
                    // Same user trying to re-register with same username and email/phone
                    if (existingUser.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(15))) {
                        // Recent registration - resend OTP
                        String verificationType = existingUser.getRegistrationType().equals("EMAIL") ?
                                "EMAIL_VERIFICATION" : "PHONE_VERIFICATION";
                        String target = existingUser.getRegistrationType().equals("EMAIL") ?
                                existingUser.getEmail() : existingUser.getContactNumber();

                        otpService.resendOtp(existingUser.getId(), target, verificationType);

                        return Map.of(
                                "status", String.valueOf(HttpStatus.OK.value()),
                                "message", "OTP resent to your " + existingUser.getRegistrationType().toLowerCase(),
                                "userId", existingUser.getId()
                        );
                    } else {
                        // Old pending registration - delete and allow fresh registration
                        userService.deleteUser(existingUser.getId());
                        otpService.deleteOtpsByUserId(existingUser.getId());
                        // Continue to create new user below
                    }
                } else {
                    // DIFFERENT user trying to register with SAME username but different email/phone
                    // Check if pending registration is old
                    if (existingUser.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(30))) {
                        // Old pending registration (30+ mins) - delete and allow new user to take username
                        userService.deleteUser(existingUser.getId());
                        otpService.deleteOtpsByUserId(existingUser.getId());
                        // Continue to create new user below
                    } else {
                        // Recent pending registration - username is temporarily reserved
                        throw new UserAlreadyExistsException("Username is temporarily reserved. Please try another username or wait.");
                    }
                }
            }
        }

        // 5. Check if email already exists (if email provided)
        if (userRequestDTO.getEmail() != null && !userRequestDTO.getEmail().isBlank()) {
            Optional<User> userByEmail = userService.findByEmail(userRequestDTO.getEmail());

            if (userByEmail.isPresent()) {
                User existingUser = userByEmail.get();

                // If email is verified, reject
                if (existingUser.getEmailVerified()) {
                    throw new UserAlreadyExistsException("Email already registered. Please login.");
                }

                // If email belongs to pending user
                if ("PENDING_VERIFICATION".equals(existingUser.getStatus())) {
                    // This should not happen if username check above worked correctly
                    // But adding as safety check

                    if (existingUser.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(15))) {
                        // Recent - update username if different and resend OTP
                        if (!existingUser.getUsername().equals(userRequestDTO.getUsername())) {
                            existingUser.setUsername(userRequestDTO.getUsername());
                            userService.saveUser(existingUser);
                        }

                        otpService.resendOtp(existingUser.getId(), existingUser.getEmail(), "EMAIL_VERIFICATION");

                        return Map.of(
                                "status", String.valueOf(HttpStatus.OK.value()),
                                "message", "OTP resent to your email",
                                "userId", existingUser.getId()
                        );
                    } else {
                        // Old pending - delete and continue
                        userService.deleteUser(existingUser.getId());
                        otpService.deleteOtpsByUserId(existingUser.getId());
                    }
                }
            }
        }

        // 6. Check if phone already exists (if phone provided)
        if (userRequestDTO.getContactNumber() != null && !userRequestDTO.getContactNumber().isBlank()) {
            Optional<User> userByPhone = userService.findByContactNumber(userRequestDTO.getContactNumber());

            if (userByPhone.isPresent()) {
                User existingUser = userByPhone.get();

                // If phone is verified, reject
                if (existingUser.getPhoneVerified()) {
                    throw new UserAlreadyExistsException("Phone number already registered. Please login.");
                }

                // If phone belongs to pending user
                if ("PENDING_VERIFICATION".equals(existingUser.getStatus())) {

                    if (existingUser.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(15))) {
                        // Recent - update username if different and resend OTP
                        if (!existingUser.getUsername().equals(userRequestDTO.getUsername())) {
                            existingUser.setUsername(userRequestDTO.getUsername());
                            userService.saveUser(existingUser);
                        }

                        otpService.resendOtp(existingUser.getId(), existingUser.getContactNumber(), "PHONE_VERIFICATION");

                        return Map.of(
                                "status", String.valueOf(HttpStatus.OK.value()),
                                "message", "OTP resent to your phone",
                                "userId", existingUser.getId()
                        );
                    } else {
                        // Old pending - delete and continue
                        userService.deleteUser(existingUser.getId());
                        otpService.deleteOtpsByUserId(existingUser.getId());
                    }
                }
            }
        }

        // 7. Determine registration type
        boolean isEmailRegistration = userRequestDTO.getEmail() != null && !userRequestDTO.getEmail().isBlank();
        String registrationType = isEmailRegistration ? "EMAIL" : "PHONE";

        // 8. All checks passed - Create new pending user
        User user = userService.createPendingUser(userRequestDTO, ipAddress, registrationType);

        // 9. Generate and send OTP
        String otpType = isEmailRegistration ? "EMAIL_VERIFICATION" : "PHONE_VERIFICATION";
        String target = isEmailRegistration ? user.getEmail() : user.getContactNumber();

        otpService.generateAndSendOtp(user.getId(), target, otpType);

        // 10. Return success response
        return Map.of(
                "status", String.valueOf(HttpStatus.CREATED.value()),
                "message", "OTP sent to your " + (isEmailRegistration ? "email" : "phone") + ". Please verify.",
                "userId", user.getId()
        );
    }

    @Override
    public Map<String, String> verifyOtp(OtpVerificationRequestDTO otpVerificationRequestDTO) {
        // Fetch the user using the provided userId
        Optional<Otp> otpData = otpService.findByUserId(otpVerificationRequestDTO.getUserId());
        if (otpData.isPresent()) {
            Optional<User> userData = userService.findByUserId(otpData.get().getUserId());
            if (userData.isPresent()) {
                User user = userData.get();
                String verificationType = user.getRegistrationType().equals("EMAIL") ? "EMAIL_VERIFICATION" : "PHONE_VERIFICATION";
                if (otpService.validateOtp(user.getId(), otpVerificationRequestDTO.getOtp(), verificationType)) {
                    user.setStatus("ACTIVE");
                    if (user.getRegistrationType().equalsIgnoreCase("Email")) {
                        user.setEmailVerified(true);
                        user.setEmailVerifiedAt(LocalDateTime.now());
                    } else {
                        user.setPhoneVerified(true);
                        user.setPhoneVerifiedAt(LocalDateTime.now());
                    }
                    userService.saveUser(user);
                    return Map.of("status", String.valueOf(HttpStatus.OK.value()), "message", "User registered successfully");
                } else {
                    return Map.of("status", String.valueOf(HttpStatus.BAD_REQUEST.value()), "message", "Invalid or expired OTP");
                }
            } else {
                return Map.of("status", String.valueOf(HttpStatus.NOT_FOUND.value()), "message", "User Details not found");
            }

        } else {
            throw new UserNotFoundException("User Details not found");
        }
    }

    @Override
    public String googleLogin(String idToken) throws GeneralSecurityException, IOException {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken googleIdToken = verifier.verify(idToken);
        if (googleIdToken == null) {
            throw new GeneralSecurityException("Invalid Google ID token.");
        }

        GoogleIdToken.Payload payload = googleIdToken.getPayload();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");

        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        } else {
            // Register new user
            user = User.builder().build();
            user.setId(UUID.randomUUID().toString());
            user.setEmail(email);
            user.setUsername(email.split("@")[0] + UUID.randomUUID().toString().substring(0, 4)); // Generate unique username
            user.setFullName(name);
            user.setAvatar(pictureUrl);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // Set a random password
            user.setEmailVerified(true);
            user.setEmailVerifiedAt(LocalDateTime.now());
            user.setStatus("ACTIVE");
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now()); // Set updatedAt when creating a new user
            user.setLastLoginAt(LocalDateTime.now());
            user.setRegistrationType("GOOGLE");
            userRepository.save(user);
        }

        return jwtService.generateToken(user);
    }
}
