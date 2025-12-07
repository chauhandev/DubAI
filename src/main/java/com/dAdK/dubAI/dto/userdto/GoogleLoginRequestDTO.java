package com.dAdK.dubAI.dto.userdto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequestDTO {
    @NotBlank(message = "ID token is required")
    private String idToken;
}