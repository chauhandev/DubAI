package com.dAdK.dubAI.controller;

import com.dAdK.dubAI.dto.userdto.UserRequestDTO;
import com.dAdK.dubAI.dto.userdto.UserResponseDTO;
import com.dAdK.dubAI.models.User;
import com.dAdK.dubAI.services.userservice.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserController userController;

    private User user1;
    private User user2;
    private UserResponseDTO userResponseDTO1;
    private UserResponseDTO userResponseDTO2;
    private UserRequestDTO userRequestDTO;

    @BeforeEach
    void setUp() {
        user1 = User.builder().build();
        user1.setId("user1");
        user1.setUsername("testuser1");
        user1.setEmail("test1@example.com");

        user2 = User.builder().build();
        user2.setId("user2");
        user2.setUsername("testuser2");
        user2.setEmail("test2@example.com");

        userResponseDTO1 = UserResponseDTO.builder()
                .id("user1")
                .username("testuser1")
                .build();

        userResponseDTO2 = UserResponseDTO.builder()
                .id("user2")
                .username("testuser2")
                .build();

        userRequestDTO = new UserRequestDTO();
        userRequestDTO.setUsername("updateduser");
        userRequestDTO.setEmail("updated@example.com");
    }

    /**
     * Tests the scenario where all users are successfully retrieved.
     * Verifies that the response status is OK and the body contains the expected list of users.
     */
    @Test
    void getAllUsers_returnsListOfUsers() {
        List<UserResponseDTO> users = Arrays.asList(userResponseDTO1, userResponseDTO2);
        when(userService.getAllUsers()).thenReturn(users);

        ResponseEntity<List<UserResponseDTO>> response = userController.getAllUsers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        assertEquals("testuser1", response.getBody().get(0).getUsername());
        verify(userService, times(1)).getAllUsers();
    }

    /**
     * Tests the scenario where an existing user is successfully retrieved by username.
     * Verifies that the response status is OK and the body contains the expected user details.
     */
    @Test
    void getUser_existingUser_returnsUser() {
        when(userService.getUser(anyString())).thenReturn(Optional.of(userResponseDTO1));

        ResponseEntity<UserResponseDTO> response = userController.getUser("testuser1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("testuser1", response.getBody().getUsername());
        verify(userService, times(1)).getUser("testuser1");
    }

    /**
     * Tests the scenario where a non-existing user is requested.
     * Verifies that the response status is NOT_FOUND and the body is null.
     */
    @Test
    void getUser_nonExistingUser_returnsNotFound() {
        when(userService.getUser(anyString())).thenReturn(Optional.empty());

        ResponseEntity<UserResponseDTO> response = userController.getUser("nonexistent");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(userService, times(1)).getUser("nonexistent");
    }

    /**
     * Tests the scenario where an existing user is successfully updated.
     * Verifies that the response status is OK and the body contains the updated user details.
     */
    @Test
    void updateUser_existingUser_returnsUpdatedUser() {
        when(userService.updateUser(anyString(), any(UserRequestDTO.class))).thenReturn(Optional.of(userResponseDTO1));

        ResponseEntity<UserResponseDTO> response = userController.updateUser("testuser1", userRequestDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("testuser1", response.getBody().getUsername()); // Assuming DTO is updated but username remains same for path
        verify(userService, times(1)).updateUser("testuser1", userRequestDTO);
    }

    /**
     * Tests the scenario where an attempt is made to update a non-existing user.
     * Verifies that the response status is NOT_FOUND and the body is null.
     */
    @Test
    void updateUser_nonExistingUser_returnsNotFound() {
        when(userService.updateUser(anyString(), any(UserRequestDTO.class))).thenReturn(Optional.empty());

        ResponseEntity<UserResponseDTO> response = userController.updateUser("nonexistent", userRequestDTO);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(userService, times(1)).updateUser("nonexistent", userRequestDTO);
    }

    /**
     * Tests the scenario where an authenticated user requests their details.
     * Verifies that the response status is OK and the body contains the user's details.
     */
    @Test
    void getDetails_authenticatedUser_returnsUserDetails() {
        User principalUser = User.builder().build();
        principalUser.setUsername("authenticatedUser");
        when(authentication.getPrincipal()).thenReturn(principalUser);
        when(userService.getUser(anyString())).thenReturn(Optional.of(userResponseDTO1));

        ResponseEntity<UserResponseDTO> response = userController.getDetails(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("testuser1", response.getBody().getUsername());
        verify(userService, times(1)).getUser("authenticatedUser");
    }

    /**
     * Tests the scenario where an unauthenticated user requests their details.
     * Verifies that the response status is UNAUTHORIZED and the body is null.
     */
    @Test
    void getDetails_unauthenticatedUser_returnsUnauthorized() {
        when(authentication.getPrincipal()).thenReturn(null); // Simulate unauthenticated

        ResponseEntity<UserResponseDTO> response = userController.getDetails(authentication);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        // Verify that the user service was never called for an unauthenticated user
        verify(userService, never()).getUser(anyString());
    }

    /**
     * Tests the scenario where an authenticated user's details are not found in the user service.
     * Verifies that the response status is NOT_FOUND and the body is null.
     */
    @Test
    void getDetails_userNotFoundInService_returnsNotFound() {
        User principalUser = User.builder().build();
        principalUser.setUsername("authenticatedUser");
        when(authentication.getPrincipal()).thenReturn(principalUser);
        when(userService.getUser(anyString())).thenReturn(Optional.empty());

        ResponseEntity<UserResponseDTO> response = userController.getDetails(authentication);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(userService, times(1)).getUser("authenticatedUser");
    }
}
