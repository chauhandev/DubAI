package com.dAdK.dubAI.services.userservice;

import com.dAdK.dubAI.dto.userdto.UserRequestDTO;
import com.dAdK.dubAI.dto.userdto.UserResponseDTO;
import com.dAdK.dubAI.models.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserService {
    List<UserResponseDTO> getAllUsers();
    Optional<UserResponseDTO> getUser(String username);
    Optional<UserResponseDTO> updateUser(String username, UserRequestDTO updateUserDTO);

    void userValidation(UserRequestDTO userRequestDTO);

    User createPendingUser(UserRequestDTO userRequestDTO, String ipAddress, String registrationType);

    Optional<User> findById(String id);

    Optional<User> findByUsername(String username);
    Optional<User> findByUserId(String userId);

    Optional<User> findByEmail(String email);

    Optional<User> findByContactNumber(String contactNumber);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByContactNumber(String contactNumber);

    void saveUser(User user);

    void deleteUser(String userId);

    List<User> findUnverifiedUsers(LocalDateTime before);


}
