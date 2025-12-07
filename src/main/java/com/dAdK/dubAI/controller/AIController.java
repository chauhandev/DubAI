package com.dAdK.dubAI.controller;

import com.dAdK.dubAI.dto.TranslationRequestDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/v3/ai")
public class AIController {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final ObjectMapper mapper;
    private final OpenAiService openAiService;

    public AIController(OpenAiService openAiService) {
        this.openAiService = openAiService;
        this.mapper = new ObjectMapper();
    }

    // ========== 🎙 TRANSCRIBE API ==========
    @PostMapping("/transcribe")
    public Map<String, Object> transcribeAudio(@RequestParam("file") MultipartFile file) throws Exception {
        File tempFile = File.createTempFile("audio-", file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(file.getBytes());
        }

        String transcription;
        if (geminiApiKey != null && !geminiApiKey.isEmpty()) {
            transcription = transcribeWithGemini(tempFile);
        } else {
            transcription = transcribeWithOpenAI(tempFile);
        }

        // Sentiment & emotion
        String analysis = analyzeText(transcription);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("modelUsed", (geminiApiKey != null && !geminiApiKey.isEmpty()) ? "Gemini" : "OpenAI");
        response.put("transcription", transcription);
        response.put("analysis", mapper.readTree(analysis));

        tempFile.delete();
        return response;
    }

    // ========== 🌐 TRANSLATE API ==========
    @PostMapping("/translate")
    public Map<String, Object> translateText(@RequestBody TranslationRequestDto request) throws Exception {
        String text = request.getText();
        String targetLanguage = request.getTargetLanguage();

        String translatedText;

        if (geminiApiKey != null && !geminiApiKey.isEmpty()) {
            translatedText = translateWithGemini(text, targetLanguage);
        } else {
            translatedText = translateWithOpenAI(text, targetLanguage);
        }

        // Sentiment & emotion analysis
        String analysis = analyzeText(translatedText);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("modelUsed", (geminiApiKey != null && !geminiApiKey.isEmpty()) ? "Gemini" : "OpenAI");
        response.put("translatedText", translatedText);
        response.put("analysis", mapper.readTree(analysis));

        return response;
    }

    // ========== 🔍 SENTIMENT + EMOTION ==========
    private String analyzeText(String text) throws Exception {
        String prompt = "Analyze this text and return JSON: {sentiment: positive|negative|neutral, emotion: one of [happy, sad, angry, surprised, calm]}.\nText: " + text;

        if (geminiApiKey != null && !geminiApiKey.isEmpty()) {
            return analyzeWithGemini(prompt);
        } else {
            return analyzeWithOpenAI(prompt);
        }
    }

    // ========== 🤖 OPENAI IMPLEMENTATION ==========
    private String transcribeWithOpenAI(File file) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost("https://api.openai.com/v1/audio/transcriptions");
            post.setHeader("Authorization", "Bearer " + openAiApiKey);

            HttpEntity entity = MultipartEntityBuilder.create()
                    .addTextBody("model", "whisper-1")
                    .addBinaryBody("file", file, ContentType.DEFAULT_BINARY, file.getName())
                    .build();

            post.setEntity(entity);

            try (CloseableHttpResponse response = client.execute(post)) {
                String result = EntityUtils.toString(response.getEntity());
                JsonNode json = mapper.readTree(result);
                return json.path("text").asText("Transcription unavailable");
            }
        }
    }

    private String translateWithOpenAI(String text, String targetLanguage) {
        ChatMessage systemMessage = new ChatMessage("system", "You are a professional translator.");
        ChatMessage userMessage = new ChatMessage("user", "Translate this to " + targetLanguage + ": " + text);

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(List.of(systemMessage, userMessage))
                .build();

        return openAiService.createChatCompletion(request)
                .getChoices().get(0).getMessage().getContent();
    }

    private String analyzeWithOpenAI(String prompt) {
        ChatMessage systemMessage = new ChatMessage("system", "You are a helpful assistant.");
        ChatMessage userMessage = new ChatMessage("user", prompt);

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(List.of(systemMessage, userMessage))
                .build();

        return openAiService.createChatCompletion(request)
                .getChoices().get(0).getMessage().getContent();
    }

    private String transcribeWithGemini(File file) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:transcribe?key=" + geminiApiKey);
            HttpEntity entity = MultipartEntityBuilder.create()
                    .addBinaryBody("file", file, ContentType.DEFAULT_BINARY, file.getName())
                    .build();
            post.setEntity(entity);

            try (CloseableHttpResponse response = client.execute(post)) {
                String result = EntityUtils.toString(response.getEntity());
                JsonNode json = mapper.readTree(result);
                return json.path("text").asText("Transcription unavailable");
            }
        }
    }

    private String translateWithGemini(String text, String targetLanguage) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey);
            post.setHeader("Content-Type", "application/json");

            String json = String.format("""
                    {
                      "contents": [{
                        "parts": [{"text": "Translate this text to %s: %s"}]
                      }]
                    }
                    """, targetLanguage, text);

            post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            try (CloseableHttpResponse response = client.execute(post)) {
                String result = EntityUtils.toString(response.getEntity());
                JsonNode root = mapper.readTree(result);

                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode parts = candidates.get(0).path("content").path("parts");
                    if (parts.isArray() && parts.size() > 0) {
                        return parts.get(0).path("text").asText();
                    }
                }

                return "Translation unavailable: " + result;
            }
        }
    }

    private String analyzeWithGemini(String prompt) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey);
            post.setHeader("Content-Type", "application/json");

            String json = String.format("""
                    {
                      "contents": [{
                        "parts": [{"text": "%s"}]
                      }]
                    }
                    """, prompt.replace("\"", "\\\""));

            post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            try (CloseableHttpResponse response = client.execute(post)) {
                String result = EntityUtils.toString(response.getEntity());
                JsonNode root = mapper.readTree(result);

                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode parts = candidates.get(0).path("content").path("parts");
                    if (parts.isArray() && parts.size() > 0) {
                        return parts.get(0).path("text").asText();
                    }
                }

                return "{\"sentiment\":\"unknown\",\"emotion\":\"unknown\"}";
            }
        }
    }
}