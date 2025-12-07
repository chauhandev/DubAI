package com.dAdK.dubAI.services.tts;

import com.dAdK.dubAI.dto.TranslateRequest;
import com.dAdK.dubAI.dto.TtsRequest;
import com.dAdK.dubAI.enums.VoiceType;
import com.dAdK.dubAI.exceptions.TtsProcessingException;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.texttospeech.v1.*;
import com.google.genai.Client;
import com.google.genai.errors.ClientException;
import com.google.genai.types.*;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static com.dAdK.dubAI.util.WavConverter.convertPcmToWav;

@Service
public class TextToSpeechServiceImpl implements TextToSpeechService {

    private static final Logger log = LoggerFactory.getLogger(TextToSpeechServiceImpl.class);

    private TextToSpeechClient gcTtsClient;
    private Client geminiClient;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gcp.credentials.path}")
    private String gcpCredentialsPath;

    @Value("${gemini.tts.model}")
    private String GEMINI_TTS_MODEL;

    @Value("${gemini.translation.model}")
    private String GEMINI_TRANSLATION_MODEL;


    @PostConstruct
    public void init() {
        initializeGoogleCloudTtsClient();
        initializeGeminiClient();
    }

    private void initializeGoogleCloudTtsClient() {
        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(gcpCredentialsPath));
            TextToSpeechSettings settings = TextToSpeechSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();
            this.gcTtsClient = TextToSpeechClient.create(settings);
            log.info("Google Cloud TextToSpeechClient initialized successfully.");
        } catch (IOException e) {
            log.error("Failed to initialize Google Cloud TextToSpeechClient.", e);
        }
    }

    private void initializeGeminiClient() {
        try {
            if (geminiApiKey != null && !geminiApiKey.isBlank()) {
                this.geminiClient = new Client.Builder()
                        .apiKey(geminiApiKey)
                        .build();
                log.info("Gemini Client initialized successfully.");
            } else {
                log.warn("Gemini API key is not configured. Gemini TTS will not be available.");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Gemini Client.", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (gcTtsClient != null) {
            gcTtsClient.close();
            log.info("Google Cloud TextToSpeechClient closed.");
        }
    }

    @Override
    public byte[] generateSpeech(TtsRequest request) {

        String voiceId = request.voiceType().getVoiceIdentifier();
        log.info("Generating speech for text ({} chars) with voiceType ID: {}", request.text().length(), voiceId);

        try {
            return voiceId.contains("Wavenet")
                    ? generateWaveNetSpeech(request.text(), request.voiceType(), request.language(), request.userPrompt(), request.translatedText(), false)
                    : generateGeminiSpeech(request.text(), request.voiceType(), GEMINI_TTS_MODEL, request.language(), request.userPrompt(), request.emotion(), request.translatedText(), false);
        } catch (Exception e) {
            if (e instanceof TtsProcessingException) {
                throw e;
            }
            log.error("Unexpected error in generateSpeech: {}", e.getMessage(), e);
            throw new TtsProcessingException("Unexpected error occurred during speech generation: " + e.getMessage(), e);
        }
    }


    @Override
    public String translateText(TranslateRequest translateRequest) {
        try {
            return getTranslatedText(translateRequest);
        } catch (Exception e) {
            log.error("Error translating text: {}", e.getMessage(), e);
            return translateRequest.text(); // Fallback to original text on error
        }
    }

    /**
     * Generates speech using Google Cloud Text-to-Speech (WaveNet) API.
     */
    private byte[] generateWaveNetSpeech(String text, VoiceType voiceType, String language, String userPrompt, String translatedText, Boolean previewOnly) {
        try {
            TranslateRequest translateRequest = new TranslateRequest(
                    text,
                    language,
                    userPrompt,
                    translatedText,
                    previewOnly
            );

            String textForTts = getTranslatedText(translateRequest);

            SynthesisInput input = SynthesisInput.newBuilder().setText(textForTts).build();

            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                    .setLanguageCode(getLanguageCode(voiceType))
                    .setName(voiceType.getVoiceIdentifier())
                    .setSsmlGender(mapGender(voiceType.getGender()))
                    .build();

            AudioConfig audioConfig = AudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.MP3)
                    .build();

            com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse response =
                    gcTtsClient.synthesizeSpeech(input, voice, audioConfig);

            ByteString audioContents = response.getAudioContent();
            byte[] audioData = audioContents.toByteArray();

            if (audioData.length == 0) {
                log.warn("WaveNet TTS returned empty audio data for voiceType {}.", voiceType.name());
                return null;
            }

            log.info("Successfully generated {} bytes of WaveNet audio data for voiceType {}.",
                    audioData.length, voiceType.name());
            return audioData;

        } catch (Exception e) {
            log.error("Error generating WaveNet speech for voiceType {}: {}",
                    voiceType.name(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Generates speech using Gemini TTS API and converts PCM to WAV format.
     */
    private byte[] generateGeminiSpeech(String text, VoiceType voiceType, String modelId, String language, String userPrompt, String emotion, String translatedText, Boolean previewOnly) {
        if (geminiClient == null) {
            log.error("Gemini client is not initialized. Cannot generate Gemini speech.");
            throw new TtsProcessingException("Gemini TTS service is not available. Please check API configuration.");
        }

        try {
            GenerateContentResponse response = callGeminiTtsApi(text, voiceType, modelId, language, userPrompt, emotion, translatedText, previewOnly);
            byte[] pcmData = extractPcmDataFromResponse(response, voiceType);

            if (pcmData == null) {
                throw new TtsProcessingException("Failed to extract audio data from Gemini TTS response for voiceType: " + voiceType.name());
            }

            byte[] wavData = convertPcmToWav(pcmData);
            log.info("Successfully generated {} bytes of Gemini WAV audio data for voiceType {}.",
                    wavData.length, voiceType.name());
            return wavData;

        } catch (ClientException e) {
            // Handle specific Gemini API errors
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown Gemini API error";
            log.error("Gemini TTS API error for voiceType {}: {}", voiceType.name(), errorMessage, e);

            if (errorMessage.contains("429") || errorMessage.contains("quota exceeded") || errorMessage.contains("RATE_LIMIT_EXCEEDED")) {
                throw new TtsProcessingException("Gemini TTS quota exceeded. " +
                        "Please check your billing plan or retry after the quota resets.", e);
            } else if (errorMessage.contains("rate limit") || errorMessage.contains("QUOTA_EXCEEDED")) {
                throw new TtsProcessingException("Gemini TTS rate limit exceeded. " +
                        "Please reduce request frequency or upgrade your plan.", e);
            } else if (errorMessage.contains("PERMISSION_DENIED") || errorMessage.contains("401")) {
                throw new TtsProcessingException("Gemini TTS authentication failed. Please check your API key.", e);
            } else if (errorMessage.contains("RESOURCE_EXHAUSTED")) {
                throw new TtsProcessingException("Gemini TTS resources are currently unavailable. Please try again later.", e);
            } else if (errorMessage.contains("INVALID_ARGUMENT") || errorMessage.contains("400")) {
                throw new TtsProcessingException("Invalid request to Gemini TTS. Please check your input parameters.", e);
            } else if (errorMessage.contains("INTERNAL") || errorMessage.contains("500")) {
                throw new TtsProcessingException("Gemini TTS service internal error. Please try again later.", e);
            } else {
                throw new TtsProcessingException("Gemini TTS request failed: " + errorMessage, e);
            }
        } catch (Exception e) {
            log.error("Unexpected error generating Gemini speech for voiceType {}: {}",
                    voiceType.name(), e.getMessage(), e);
            throw new TtsProcessingException("Unexpected error in Gemini TTS processing: " + e.getMessage(), e);
        }
    }

    private GenerateContentResponse callGeminiTtsApi(
            String text,
            VoiceType voiceType,
            String modelId,
            String language,
            String userPrompt,
            String emotion,
            String translatedText,
            Boolean previewOnly
    ) {
        TranslateRequest translateRequest = new TranslateRequest(
                text,
                language,
                userPrompt,
                translatedText,
                previewOnly
        );
        // --- STEP 1: Translate the text to the specified language ---
        // This ensures the TTS model receives text in the correct script.
        String textForTts = getTranslatedText(translateRequest);

        // --- Configure the voiceType ---
        PrebuiltVoiceConfig prebuiltVoiceConfig = PrebuiltVoiceConfig.builder()
                .voiceName(voiceType.getVoiceIdentifier())
                .build();

        VoiceConfig voiceConfig = VoiceConfig.builder()
                .prebuiltVoiceConfig(prebuiltVoiceConfig)
                .build();

        // --- Configure speech with optional language ---
        SpeechConfig.Builder speechConfigBuilder = SpeechConfig.builder()
                .voiceConfig(voiceConfig);

        if (language != null && !language.isBlank()) {
            // Crucial: Set the language code for the TTS engine configuration
            speechConfigBuilder.languageCode(language);
        }

        SpeechConfig speechConfig = speechConfigBuilder.build();

        // --- Build the generation config for audio output ---
        GenerateContentConfig config = GenerateContentConfig.builder()
                .responseModalities("AUDIO")
                .speechConfig(speechConfig)
                .build();

        // --- Construct the prompt dynamically ---
        StringBuilder promptBuilder = new StringBuilder();

        // Note: The specific language instruction is now less critical since the text is already translated,
        // but it still serves as a reinforcement for the TTS model's accent/reading.
        // Define the desired role and core task upfront
        String basePrompt = "You are a professional Text-to-Speech (TTS) voice generator. Your task is to vocalize the provided text exactly, adhering to all style, emotion, and language instructions.";
        promptBuilder.append(basePrompt);

        // 1. Language Instruction (Highest Priority for correct pronunciation)
        if (language != null && !language.isBlank()) {
            String languageName = getLanguageName(language);
            promptBuilder.append(" Please generate the audio in **")
                    .append(languageName)
                    .append("**.")
                    .append(" Speak naturally in this language.");
        }

        // 2. Emotion/Tone/Style Instructions (Combined for clarity)
        boolean addedTone = false;
        if (emotion != null && !emotion.isBlank()) {
            promptBuilder.append(" Convey the **emotion** of '")
                    .append(emotion)
                    .append("' in your voice.");
            addedTone = true;
        }
        if (userPrompt != null && !userPrompt.isBlank()) {
            if (!addedTone) { // Add a separator only if emotion wasn't just added
                promptBuilder.append(" Also,");
            }
            // UserPrompt gives the speaking style, tone, or delivery instruction
            promptBuilder.append(" Read the text in a way that matches this **delivery instruction**: '")
                    .append(userPrompt)
                    .append("'.");
        }

        // 3. The Text to Be Read (Clear call-to-action)
        promptBuilder.append(" Here is the text to convert into speech: \"")
                .append(textForTts)
                .append("\".");

        // Add a final instruction for clarity/completion
        promptBuilder.append(" Do not add any commentary or extra text.");

        Content content = Content.builder()
                .parts(Part.fromText(promptBuilder.toString()))
                .build();

        // --- Call the model ---
        return geminiClient.models.generateContent(modelId, List.of(content), config);
    }


    private String getTranslatedText(TranslateRequest translateRequest) {
        // Early returns for invalid inputs
        if (translateRequest.language() == null || translateRequest.language().isBlank() || translateRequest.text() == null || translateRequest.text().isBlank()) {
            return translateRequest.text();
        }

        // Build prompt based on conditions
        String prompt = buildPrompt(translateRequest);

        // Call Gemini API
        return callGeminiAPI(prompt, translateRequest.text(), GEMINI_TRANSLATION_MODEL);
    }

    private String buildPrompt(TranslateRequest translateRequest) {
        StringBuilder prompt = new StringBuilder();
        String languageName = getLanguageName(translateRequest.language());

        // Case 1: Preview mode with no existing translation
        if (translateRequest.previewOnly() && translateRequest.translatedText().isEmpty()) {
            prompt.append(String.format("""
                    You are an expert translator. Translate the text below into %s.
                    
                    **Requirements:**
                    - Preserve meaning, tone, and intent precisely
                    - Maintain register (formal/informal/technical) and style
                    - Adapt idioms naturally—avoid literal translation if unnatural
                    - Keep formatting intact
                    - Use standard terminology for technical/industry terms
                    - Localize cultural references when clear equivalents exist
                    - Ensure grammatical correctness and natural fluency
                    
                    **Output:** Translated text only, no explanations.
                    
                    ---
                    
                    **Text:** %s""", languageName, translateRequest.text()));
        }
        // Case 2: Preview mode with existing translation - return early in parent method
        else if (translateRequest.previewOnly() && !translateRequest.translatedText().isEmpty()) {
            return null; // Signal to return translatedText directly
        }
        // Case 3: Non-preview with existing translation - romanize
        else if (!translateRequest.previewOnly() && !translateRequest.translatedText().isEmpty()) {
            prompt.append(String.format("""
                    You are a translation assistant.
                    
                    Your task is to re-write the entire text **using the Roman (English) alphabet**.
                    Respond **only** with the re-written text — no explanations, notes, or extra output.
                    
                    Text:
                    \"\"\"%s\"\"\"""", translateRequest.translatedText()));
        }
        // Case 4: Non-preview without existing translation - translate and romanize
        else {
            prompt.append(String.format("""
                    You are a translation assistant.
                    
                    Your task is to identify language from given text and translate into **%s**, but write the entire translation **using the Roman (English) alphabet**.
                    Do not use any native script characters of the target language.
                    Respond **only** with the translated text — no explanations, notes, or extra output.
                    
                    Text:
                    \"\"\"%s\"\"\"""", languageName, translateRequest.text()));
        }

        // Append user prompt if provided
        if (!translateRequest.userPrompt().isEmpty() && prompt.length() > 0) {
            String instruction = translateRequest.text().isEmpty() ? "translating" : "re-writing";
            prompt.append(String.format("""                     
                            After %s, please **modify the %s** according to this additional user instruction:
                            \"\"\"%s\"\"\"""",
                    instruction,
                    translateRequest.text().isEmpty() ? "translation" : "text",
                    translateRequest.userPrompt()));
        }

        return prompt.toString();
    }

    private String callGeminiAPI(String prompt, String fallbackText, String modelId) {
        // Handle early return case
        if (prompt == null) {
            return fallbackText;
        }

        try {
            Content content = Content.builder()
                    .parts(Part.fromText(prompt))
                    .build();

            GenerateContentConfig config = GenerateContentConfig.builder().build();

            GenerateContentResponse response = geminiClient.models.generateContent(
                    modelId,
                    List.of(content),
                    config
            );

            return response.text();

        } catch (Exception e) {
            System.err.println("Error during translation: " + e.getMessage());
            e.printStackTrace(); // Added for better debugging
            return fallbackText;
        }
    }

    /**
     * Maps language code to human-readable language name for prompts.
     */
    private String getLanguageName(String languageCode) {
        if (languageCode == null) {
            return "English";
        }
        return switch (languageCode.toLowerCase()) {
            case "en-in" -> "English (India)";
            case "en-us" -> "English (United States)";
            case "en" -> "English";
            case "es" -> "Spanish";
            case "fr" -> "French";
            case "de" -> "German";
            case "zh" -> "Chinese";
            case "hi", "hin" -> "Hindi";
            case "ja", "jp" -> "Japanese";
            case "ko" -> "Korean";
            case "gu-in" -> "Gujarati (India)";
            case "kn-in" -> "Kannada (India)";
            case "kok-in" -> "Konkani (India)";
            case "mai-in" -> "Maithili (India)";
            case "ml-in" -> "Malayalam (India)";
            case "or-in" -> "Odia (India)";
            case "pa-in" -> "Punjabi (India)";
            case "sd-in" -> "Sindhi (India)";

            // Fallback for short codes without region
            case "gu" -> "Gujarati (India)";
            case "kn" -> "Kannada (India)";
            case "kok" -> "Konkani (India)";
            case "mai" -> "Maithili (India)";
            case "ml" -> "Malayalam (India)";
            case "or" -> "Odia (India)";
            case "pa" -> "Punjabi (India)";
            case "sd" -> "Sindhi (India)";

            default -> "English";
        };
    }


    /**
     * Extracts PCM audio data from Gemini API response.
     */
    private byte[] extractPcmDataFromResponse(GenerateContentResponse response, VoiceType voiceType) {
        if (response.parts() == null || response.parts().isEmpty()) {
            log.warn("No parts found in Gemini TTS response for voiceType {}", voiceType.name());
            return null;
        }

        Part firstPart = response.parts().get(0);

        if (firstPart.inlineData().isEmpty() || firstPart.inlineData().get().data().isEmpty()) {
            log.warn("No inline data found in Gemini TTS response for voiceType {}", voiceType.name());
            return null;
        }

        byte[] pcmData = firstPart.inlineData().get().data().get();

        if (pcmData.length == 0) {
            log.warn("Gemini TTS returned empty audio data for voiceType {}.", voiceType.name());
            return null;
        }

        log.info("Extracted {} bytes of PCM data from Gemini response", pcmData.length);
        return pcmData;
    }


    /**
     * Maps gender string to Google Cloud TTS gender enum.
     */
    private SsmlVoiceGender mapGender(String gender) {
        return switch (gender.toUpperCase()) {
            case "MALE" -> SsmlVoiceGender.MALE;
            case "FEMALE" -> SsmlVoiceGender.FEMALE;
            default -> SsmlVoiceGender.NEUTRAL;
        };
    }

    /**
     * Extracts language code from voiceType type identifier.
     */
    private String getLanguageCode(VoiceType voiceType) {
        String id = voiceType.getVoiceIdentifier();

        // Parse WaveNet voiceType IDs (e.g., "en-US-Wavenet-D" -> "en-US")
        if (id.contains("Wavenet")) {
            int secondDashIndex = id.indexOf('-');
            int thirdDashIndex = id.indexOf('-', secondDashIndex + 1);
            if (thirdDashIndex > 0) {
                return id.substring(0, thirdDashIndex);
            }
        }

        // Fallback for specific voiceType types
        return switch (voiceType) {
            case EN_US_MALE, EN_US_FEMALE -> "en-US";
            case EN_GB_FEMALE -> "en-GB";
            case ES_ES_MALE -> "es-ES";
            case JA_JP_FEMALE -> "ja-JP";
            default -> "en-US";
        };
    }
}
