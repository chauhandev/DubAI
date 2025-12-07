package com.dAdK.dubAI.dto.userdto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponseDTO {
    private String id;
    private String username;
    private String fullName;
    private String gender;
    private String dateOfBirth;
    private String email;
    private LocalDateTime lastLoginAt;
}
