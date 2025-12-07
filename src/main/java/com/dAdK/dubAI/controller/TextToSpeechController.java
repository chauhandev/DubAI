package com.dAdK.dubAI.controller;

import com.dAdK.dubAI.dto.ApiResponse;
import com.dAdK.dubAI.dto.TranslateRequest;
import com.dAdK.dubAI.dto.TtsRequest;
import com.dAdK.dubAI.dto.audioanalysis.compressedaudio.CompressedAudio;
import com.dAdK.dubAI.services.tts.AudioCompressionService;
import com.dAdK.dubAI.services.tts.TextToSpeechService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tts")
public class TextToSpeechController {
    private static final Logger logger = LoggerFactory.getLogger(TextToSpeechController.class);
    private final TextToSpeechService textToSpeechService;
    private final AudioCompressionService audioCompressionService;

    public TextToSpeechController(TextToSpeechService textToSpeechService, AudioCompressionService audioCompressionService) {
        this.textToSpeechService = textToSpeechService;
        this.audioCompressionService = audioCompressionService;
    }

    @PostMapping("/translate")
    public ResponseEntity<ApiResponse<String>> translateText(@RequestBody TranslateRequest request) {
        String translatedText = textToSpeechService.translateText(request);
        ApiResponse<String> response = ApiResponse.success(translatedText, "Translation successful");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate/speech")
    public ResponseEntity<?> generateSpeech(
            @RequestBody TtsRequest request,
            @RequestParam(defaultValue = "original") String mode,        // original | compressed
            @RequestParam(defaultValue = "opus") String format,          // opus | mp3
            @RequestParam(required = false) Integer bitrate,             // only for mp3
            @RequestParam(defaultValue = "VOICE_HIGH")
            AudioCompressionService.CompressionQuality quality) {

        ApiResponse<CompressedAudio> response;

        if ("compressed".equalsIgnoreCase(mode)) {

            if ("mp3".equalsIgnoreCase(format)) {
                int effectiveBitrate = (bitrate != null) ? bitrate : 128_000;
                response = audioCompressionService.generateCompressedMP3(request, effectiveBitrate);

            } else {
                response = audioCompressionService.generateCompressedSpeech(request, quality);
            }

        } else {
            response = audioCompressionService.generateOriginalAudio(request);
        }

        if (!response.isSuccess()) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }

        CompressedAudio audio = response.getData();
        String contentType = switch (audio.getMetadata().getFormat()) {
            case "mp3" -> "audio/mpeg";
            case "opus" -> "audio/opus";
            default -> "audio/wav";
        };

        return buildAudioResponse(audio, contentType);
    }


    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        boolean encoderAvailable = audioCompressionService.isEncoderAvailable();

        if (encoderAvailable) {
            return ResponseEntity.ok(ApiResponse.success("TTS service is running with compression enabled"));
        } else {
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("TTS service is running but audio compression is unavailable"));
        }
    }

    /**
     * Helper method to build audio response with proper headers
     */
    private ResponseEntity<byte[]> buildAudioResponse(CompressedAudio audio, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(contentType));
        headers.setContentLength(audio.getAudioData().length);
        headers.setCacheControl("public, max-age=86400");

        // Add compression metadata to headers
        audio.getHeaderMap().forEach(headers::set);

        // Set filename for download
        String extension = audio.getMetadata().getFormat();
        headers.setContentDispositionFormData("attachment", "audio." + extension);

        return new ResponseEntity<>(audio.getAudioData(), headers, HttpStatus.OK);
    }
}