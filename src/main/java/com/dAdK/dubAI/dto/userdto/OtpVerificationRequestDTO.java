package com.dAdK.dubAI.dto.userdto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpVerificationRequestDTO {
    @NotBlank
    private String userId;
    @NotBlank
    private String otp;
}
