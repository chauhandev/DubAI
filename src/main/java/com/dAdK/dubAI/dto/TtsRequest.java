package com.dAdK.dubAI.dto;

import com.dAdK.dubAI.enums.VoiceType;

public record TtsRequest(String text, VoiceType voiceType, String language , String userPrompt , String emotion, String translatedText, Boolean previewOnly) {
}
