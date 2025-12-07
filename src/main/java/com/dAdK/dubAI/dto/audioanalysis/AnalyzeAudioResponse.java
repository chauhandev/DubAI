package com.dAdK.dubAI.dto.audioanalysis;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalyzeAudioResponse {
    private String transcription;
    private String sentiment;
    private String tone;
    private String pitch;
    private String emotion;
    private String voiceType;
}