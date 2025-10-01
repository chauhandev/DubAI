package com.dAdK.dubAI.dto.userdto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDTO {
    @NotBlank
    private String identifier;
    @NotBlank
    private String password;
}