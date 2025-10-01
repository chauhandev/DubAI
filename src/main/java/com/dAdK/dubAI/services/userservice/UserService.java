package com.dAdK.dubAI.services.userservice;

import com.dAdK.dubAI.dto.userdto.UserRequestDTO;
import com.dAdK.dubAI.dto.userdto.UserResponseDTO;
import com.dAdK.dubAI.models.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    List<UserResponseDTO> getAllUsers();
    Optional<UserResponseDTO> getUser(String username);
    Optional<User> findUserById(String userId); // Added to fetch User object by ID
    UserResponseDTO createUser(UserRequestDTO userRequestDTO);
    User createPendingUser(UserRequestDTO userRequestDTO); // Added for creating pending users
    void activateUser(String userId); // Added for activating users
    Optional<UserResponseDTO> updateUser(String username, UserRequestDTO updateUserDTO);
    void userValidation(UserRequestDTO userRequestDTO);
    void saveUser(User user);
}
