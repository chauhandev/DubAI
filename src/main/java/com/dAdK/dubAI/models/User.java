package com.dAdK.dubAI.models;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@Document(collection = "users")
public class User {
    @Id
    private String id;
    @Indexed(unique = true)
    private String username;
    private String fullName;
    private String avatar;

    @Indexed(unique = true, sparse = true)
    private String email;

    @Indexed(unique = true, sparse = true)
    private String contactNumber;
    private String password;
    private String gender;
    private String address;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate dateOfBirth;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime lastLoginAt;

    @Indexed
    @Builder.Default
    private String status = "PENDING_VERIFICATION";

    @Builder.Default
    private Boolean emailVerified = false;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime emailVerifiedAt;

    @Builder.Default
    private Boolean phoneVerified = false;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime phoneVerifiedAt;


    private String registrationType; // "EMAIL" or "PHONE"
    private String registrationIp;
    private String lastLoginIp;

    @Builder.Default
    private Boolean deleted = false;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime deletedAt;
}
