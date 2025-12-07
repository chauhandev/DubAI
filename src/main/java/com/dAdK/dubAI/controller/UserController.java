package com.dAdK.dubAI.controller;

import com.dAdK.dubAI.dto.ApiResponse;
import com.dAdK.dubAI.dto.userdto.UserRequestDTO;
import com.dAdK.dubAI.dto.userdto.UserResponseDTO;
import com.dAdK.dubAI.models.User;
import com.dAdK.dubAI.services.userservice.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User", description = "User management APIs")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Get All Users", description = "Returns list of all users")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getAllUsers() {
        List<UserResponseDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users, "Users retrieved successfully"));
    }

    @Operation(summary = "Get User", description = "Get user details")
    @GetMapping("/{username}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUser(@PathVariable String username) {
        return userService.getUser(username)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user, "User retrieved successfully")))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found")));
    }

    @Operation(summary = "Update user", description = "Update user details")
    @PutMapping("/{username}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUser(@PathVariable String username, @Valid @RequestBody UserRequestDTO userRequestDTO) {
        return userService.updateUser(username, userRequestDTO)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user, "User updated successfully")))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found")));
    }

    @Operation(summary = "Fetch user details", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/details")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getDetails(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication required"));
        }
        String userId = ((User) authentication.getPrincipal()).getUsername();
        return userService.getUser(userId)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user, "User details retrieved successfully")))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("User not found")));
    }
}
