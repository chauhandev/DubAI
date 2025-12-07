package com.dAdK.dubAI.controller;


import com.dAdK.dubAI.dto.audioanalysis.AnalyzeAudioRequest;
import com.dAdK.dubAI.dto.audioanalysis.AnalyzeAudioResponse;
import com.dAdK.dubAI.services.audioanalysisservice.AudioAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audio")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SpeechToTextController {

    private final AudioAnalysisService audioAnalysisService;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalyzeAudioResponse> analyzeAudio(
            @RequestParam("audioFile") MultipartFile file) throws IOException {

        log.info("Received audio analysis request for file: {}", file.getOriginalFilename());

        // Validate file
        validateAudioFile(file);

        // Convert file to base64 data URI
        String audioDataUri = convertToDataUri(file);

        // Create request object
        AnalyzeAudioRequest request = new AnalyzeAudioRequest(audioDataUri);

        // Perform analysis (service layer will throw AudioAnalysisException if it fails)
        AnalyzeAudioResponse response = audioAnalysisService.analyzeAudio(request);

        log.info("Audio analysis completed successfully for file: {}", file.getOriginalFilename());

        return ResponseEntity.ok(response);
    }

    /**
     * Validates the uploaded audio file.
     * Throws IllegalArgumentException for validation failures.
     */
    private void validateAudioFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please upload an audio file.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Max file size is 5MB.");
        }

        String contentType = file.getContentType();

        // Flexible validation - check if it starts with "audio/"
        if (contentType == null || !contentType.startsWith("audio/")) {
            // Fallback to extension-based validation
            String filename = file.getOriginalFilename();
            if (filename == null || !hasValidAudioExtension(filename)) {
                throw new IllegalArgumentException(
                        "Only audio files are supported (.mp3, .wav, .webm, .ogg, .mp4, .m4a)."
                );
            }
        }

        log.debug("File validation passed for: {} (type: {}, size: {} bytes)",
                file.getOriginalFilename(), contentType, file.getSize());
    }

    /**
     * Checks if the filename has a valid audio extension.
     */
    private boolean hasValidAudioExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return false;
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        List<String> supportedExtensions = List.of("mp3", "wav", "webm", "ogg", "mp4", "m4a", "aac");

        return supportedExtensions.contains(extension);
    }

    /**
     * Converts the uploaded file to a base64 data URI.
     * Throws IOException if file reading fails.
     */
    private String convertToDataUri(MultipartFile file) throws IOException {
        try {
            byte[] fileContent = file.getBytes();
            String base64Encoded = Base64.getEncoder().encodeToString(fileContent);
            String mimeType = file.getContentType();

            // Fallback MIME type if not detected
            if (mimeType == null) {
                mimeType = "audio/mpeg"; // default fallback
                log.warn("MIME type not detected, using default: {}", mimeType);
            }

            String dataUri = String.format("data:%s;base64,%s", mimeType, base64Encoded);
            log.debug("Converted file to data URI (length: {} chars)", dataUri.length());

            return dataUri;
        } catch (IOException e) {
            log.error("Failed to read audio file: {}", e.getMessage(), e);
            throw new IOException("Failed to read the audio file. Please try again.", e);
        }
    }
}