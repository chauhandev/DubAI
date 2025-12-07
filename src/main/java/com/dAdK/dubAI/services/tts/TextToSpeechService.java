package com.dAdK.dubAI.services.tts;

import com.dAdK.dubAI.dto.TranslateRequest;
import com.dAdK.dubAI.dto.TtsRequest;

public interface TextToSpeechService {
    byte[] generateSpeech(TtsRequest request);

    String translateText(TranslateRequest translateRequest);
}