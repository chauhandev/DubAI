package com.dAdK.dubAI.dto;

public record TranslateRequest(String text, String language , String userPrompt , String translatedText, Boolean previewOnly) {
}