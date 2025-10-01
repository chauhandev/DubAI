package com.dAdK.dubAI.dto.userdto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class UserResponseDTO {
    private String id;
    private String username;
    private String fullName;
    private String gender;
    private String dateOfBirth;
    private LocalDateTime lastLoginAt;

    public String getUserId() {
        return id;
    }
}
