package com.dAdK.dubAI.services.userservice;

import com.dAdK.dubAI.dto.userdto.UserRequestDTO;
import com.dAdK.dubAI.dto.userdto.UserResponseDTO;
import com.dAdK.dubAI.models.User;
import com.dAdK.dubAI.repository.UserRepository;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?[1-9]\\d{1,14}$");

    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_]{3,30}$");

    @Override
    public void userValidation(UserRequestDTO userRequestDTO) {
        log.info("Validating user input for username: {}", userRequestDTO.getUsername());

        // Validate username
        if (!USERNAME_PATTERN.matcher(userRequestDTO.getUsername()).matches()) {
            throw new ValidationException("Username must be 3-30 characters and contain only letters, numbers, and underscores");
        }

        // Validate email if provided
        if (userRequestDTO.getEmail() != null && !userRequestDTO.getEmail().isBlank()) {
            if (!EMAIL_PATTERN.matcher(userRequestDTO.getEmail()).matches()) {
                throw new ValidationException("Invalid email format");
            }
        }

        // Validate phone if provided
        if (userRequestDTO.getContactNumber() != null && !userRequestDTO.getContactNumber().isBlank()) {
            if (!PHONE_PATTERN.matcher(userRequestDTO.getContactNumber()).matches()) {
                throw new ValidationException("Invalid phone number format. Use international format (e.g., +1234567890)");
            }
        }

        // Validate password strength
        if (userRequestDTO.getPassword().length() < 8) {
            throw new ValidationException("Password must be at least 8 characters long");
        }

        if (!userRequestDTO.getPassword().matches(".*[A-Z].*")) {
            throw new ValidationException("Password must contain at least one uppercase letter");
        }

        if (!userRequestDTO.getPassword().matches(".*[a-z].*")) {
            throw new ValidationException("Password must contain at least one lowercase letter");
        }

        if (!userRequestDTO.getPassword().matches(".*[0-9].*")) {
            throw new ValidationException("Password must contain at least one digit");
        }

        // Validate at least one contact method
        if (!userRequestDTO.hasEmailOrPhone()) {
            throw new ValidationException("Either email or phone number must be provided");
        }

        log.info("User input validation successful for username: {}", userRequestDTO.getUsername());
    }

    @Override
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToUserResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserResponseDTO> getUser(String username) {
        return userRepository.findByUsername(username)
                .map(this::convertToUserResponseDTO);
    }

    @Override
    public Optional<UserResponseDTO> updateUser(String username, UserRequestDTO updateUserDTO) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    user.setFullName(updateUserDTO.getFullName());
                    user.setGender(updateUserDTO.getGender());
                    user.setDateOfBirth(updateUserDTO.getDateOfBirth());
                    user.setEmail(updateUserDTO.getEmail());
                    user.setContactNumber(updateUserDTO.getContactNumber());
                    user.setAddress(updateUserDTO.getAddress());
                    return convertToUserResponseDTO(userRepository.save(user));
                });
    }

    private UserResponseDTO convertToUserResponseDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null)
                .lastLoginAt(user.getLastLoginAt())
                .email(user.getEmail())
                .build();
    }

    @Override
    @Transactional
    public User createPendingUser(UserRequestDTO userRequestDTO, String ipAddress, String registrationType) {
        log.info("Creating pending user with username: {}, registrationType: {}",
                userRequestDTO.getUsername(), registrationType);

        User user = User.builder()
                .username(userRequestDTO.getUsername())
                .fullName(userRequestDTO.getFullName())
                .email(userRequestDTO.getEmail())
                .contactNumber(userRequestDTO.getContactNumber())
                .password(passwordEncoder.encode(userRequestDTO.getPassword()))
                .gender(userRequestDTO.getGender())
                .address(userRequestDTO.getAddress())
                .dateOfBirth(userRequestDTO.getDateOfBirth())
                .status("PENDING_VERIFICATION")
                .emailVerified(false)
                .phoneVerified(false)
                .registrationType(registrationType)
                .createdAt(LocalDateTime.now())
                .registrationIp(ipAddress)
                .deleted(false)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Pending user created successfully with ID: {}", savedUser.getId());

        return savedUser;
    }

    @Override
    public Optional<User> findById(String id) {
        log.debug("Finding user by ID: {}", id);
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        log.debug("Finding user by username: {}", username);
        return userRepository.findByUsernameAndDeletedFalse(username);
    }

    @Override
    public Optional<User> findByUserId(String userId) {
        return userRepository.findById(userId);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmailAndDeletedFalse(email);
    }

    @Override
    public Optional<User> findByContactNumber(String contactNumber) {
        if (contactNumber == null || contactNumber.isBlank()) {
            return Optional.empty();
        }
        log.debug("Finding user by contact number: {}", contactNumber);
        return userRepository.findByContactNumberAndDeletedFalse(contactNumber);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByContactNumber(String contactNumber) {
        if (contactNumber == null || contactNumber.isBlank()) {
            return false;
        }
        return userRepository.existsByContactNumber(contactNumber);
    }

    @Override
    @Transactional
    public void saveUser(User user) {
        log.info("Saving user with ID: {}", user.getId());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(String userId) {
        log.info("Deleting user with ID: {}", userId);
        userRepository.deleteById(userId);
    }

    @Override
    public List<User> findUnverifiedUsers(LocalDateTime before) {
        log.debug("Finding unverified users created before: {}", before);
        return userRepository.findByStatusAndCreatedAtBefore("PENDING_VERIFICATION", before);
    }

}