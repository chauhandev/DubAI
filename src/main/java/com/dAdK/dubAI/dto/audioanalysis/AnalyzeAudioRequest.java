package com.dAdK.dubAI.dto.audioanalysis;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalyzeAudioRequest {
    @NotBlank(message = "Audio data URI is required")
    private String audioDataUri;
}