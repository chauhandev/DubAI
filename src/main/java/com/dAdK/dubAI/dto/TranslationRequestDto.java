package com.dAdK.dubAI.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TranslationRequestDto {
    private String text;
    private String targetLanguage;
}