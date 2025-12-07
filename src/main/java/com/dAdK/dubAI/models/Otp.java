package com.dAdK.dubAI.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "otps")
public class Otp {
    @Id
    private String id;

    @Indexed
    private String userId;

    private String otp;

    @Indexed
    private String type; // EMAIL_VERIFICATION, PHONE_VERIFICATION, PASSWORD_RESET

    @Builder.Default
    private Integer attemptCount = 0;

    @Builder.Default
    private Integer maxAttempts = 5;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    @Indexed // MongoDB TTL index - auto delete after expiry
    private LocalDateTime expiresAt;

    @Builder.Default
    private Boolean used = false;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime usedAt;

    private User user;
}
