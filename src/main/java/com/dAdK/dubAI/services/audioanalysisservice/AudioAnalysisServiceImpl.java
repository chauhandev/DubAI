package com.dAdK.dubAI.services.audioanalysisservice;


import com.dAdK.dubAI.dto.audioanalysis.AnalyzeAudioRequest;
import com.dAdK.dubAI.dto.audioanalysis.AnalyzeAudioResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioAnalysisServiceImpl implements AudioAnalysisService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${google.ai.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent}")
    private String apiUrl;

    public AnalyzeAudioResponse analyzeAudio(AnalyzeAudioRequest request) {
        try {
            // Prepare the API request
            Map<String, Object> requestBody = buildGeminiRequest(request.getAudioDataUri());

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Make API call
            String url = apiUrl + "?key=" + apiKey;
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // Parse response
            return parseGeminiResponse(response.getBody());

        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to analyze audio", e);
        }
    }

    private Map<String, Object> buildGeminiRequest(String audioDataUri) {
        String prompt = """
                You are an expert audio analyst. For each audio input, perform complete analysis and return structured JSON.
                
                **Tasks:**
                1. **Transcription** – Convert audio to accurate text
                2. **Speech Analysis** – Assess pace, loudness, clarity, intonation, expressiveness
                3. **Sentiment** – Classify as: Positive, Negative, or Neutral
                4. **Tone** – Identify: Joyful, Angry, Sad, Calm, or Excited
                5. **Pitch** – Categorize: High, Medium, or Low
                6. **Emotion** – Select best match: Neutral, Joyful, Somber, Excited, Angry, Sad, Fearful, Surprised, Calm, Whispering
                7. **Voice Type** – Select the best matching voice name. Output format: just the name (e.g., CHARON, DESPINA).
                   - **Female:** AOEDE (Storyteller/silky), CALLIRRHOE (Warm/friendly), DESPINA (Comforting), KORE (Executive/commanding), LEDA (Millennial/energetic), VINDEMIATRIX (Sophisticated/wise), ZEPHYR (Bubbly/enthusiastic)
                   - **Male:** ACHIRD (High-energy), ALGENIB (Deep bass), CHARON (Authoritative), ENCELADUS (Intimate/ASMR), FENRIR (Sports hype), IAPETUS (Trustworthy), ORUS (Distinguished/luxury), PUCK (Playful/comedy), UMBRIEL (Easygoing/podcast)
                
                **Output JSON:**
                {
                  "transcription": "",
                  "sentiment": "",
                  "tone": "",
                  "pitch": "",
                  "emotion": "",
                  "voiceType": ""
                }""";

        // Parse data URI
        String[] parts = audioDataUri.split(",");
        String mimeType = parts[0].split(";")[0].replace("data:", "");
        String base64Data = parts[1];

        Map<String, Object> inlineData = new HashMap<>();
        inlineData.put("mimeType", mimeType);
        inlineData.put("data", base64Data);

        Map<String, Object> audioPart = new HashMap<>();
        audioPart.put("inlineData", inlineData);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(textPart, audioPart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));

        return requestBody;
    }

    private AnalyzeAudioResponse parseGeminiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String textContent = root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            // Extract JSON from markdown code blocks if present
            textContent = textContent.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

            JsonNode analysisJson = objectMapper.readTree(textContent);

            return AnalyzeAudioResponse.builder()
                    .transcription(analysisJson.path("transcription").asText())
                    .sentiment(analysisJson.path("sentiment").asText())
                    .tone(analysisJson.path("tone").asText())
                    .pitch(analysisJson.path("pitch").asText())
                    .emotion(analysisJson.path("emotion").asText())
                    .voiceType(analysisJson.path("voiceType").asText())
                    .build();

        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse API response", e);
        }
    }
}