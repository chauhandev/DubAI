package com.dAdK.dubAI.services.impl;

import com.dAdK.dubAI.dto.userdto.UserRequestDTO;
import com.dAdK.dubAI.dto.userdto.UserResponseDTO;
import com.dAdK.dubAI.exceptions.UserAlreadyExistsException;
import com.dAdK.dubAI.models.User;
import com.dAdK.dubAI.repository.UserRepository;
import com.dAdK.dubAI.services.userservice.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    public Optional<User> findUserById(String userId) {
        return userRepository.findById(userId); // Use findById for MongoDB
    }

    @Override
    public UserResponseDTO createUser(UserRequestDTO userRequestDTO) {
        User user = buildUserFromRequest(userRequestDTO, "ACTIVE");
        User savedUser = userRepository.save(user);
        return convertToUserResponseDTO(savedUser);
    }

    @Override
    public User createPendingUser(UserRequestDTO userRequestDTO) {
        return buildUserFromRequest(userRequestDTO, "PENDING");
    }

    @Override
    public void activateUser(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus("ACTIVE"); // Activate user
            userRepository.save(user);
        });
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

    @Override
    public void userValidation(UserRequestDTO userRequestDTO) {
        if (userRepository.findByUsername(userRequestDTO.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException("Username already exists: " + userRequestDTO.getUsername());
        }
        if (userRequestDTO.getEmail() != null && userRepository.findByEmail(userRequestDTO.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email already exists: " + userRequestDTO.getEmail());
        }

        if (userRequestDTO.getContactNumber() != null && userRepository.findByContactNumber(userRequestDTO.getContactNumber()).isPresent()) {
            throw new UserAlreadyExistsException("Contact number already exists: " + userRequestDTO.getContactNumber());
        }
    }

    private UserResponseDTO convertToUserResponseDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null)
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    private User convertToUser(UserResponseDTO userResponseDTO) {
        return User.builder()
                .id(userResponseDTO.getId())
                .username(userResponseDTO.getUsername())
                .fullName(userResponseDTO.getFullName())
                .gender(userResponseDTO.getGender())
                .dateOfBirth(userResponseDTO.getDateOfBirth() != null
                        ? LocalDate.parse(userResponseDTO.getDateOfBirth())
                        : null)
                .lastLoginAt(userResponseDTO.getLastLoginAt())
                .build();
    }


    // Helper method to build User object from DTO
    private User buildUserFromRequest(UserRequestDTO userRequestDTO, String status) {
        User.UserBuilder userBuilder = User.builder()
                .username(userRequestDTO.getUsername())
                .fullName(userRequestDTO.getFullName())
                .gender(userRequestDTO.getGender())
                .dateOfBirth(userRequestDTO.getDateOfBirth())
                .email(userRequestDTO.getEmail())
                .contactNumber(userRequestDTO.getContactNumber())
                .address(userRequestDTO.getAddress())
                .password(passwordEncoder.encode(userRequestDTO.getPassword()))
                .createdAt(LocalDateTime.now())
                .lastLoginAt(LocalDateTime.now())
                .status(status);

        if ("PENDING".equals(status)) {
            userBuilder.id(java.util.UUID.randomUUID().toString()); // Generate a UUID for pending users
        }

        return userBuilder.build();
    }

    @Override
    public void saveUser(User user) {
        userRepository.save(user);
    }
}
