package com.dAdK.dubAI.services.tts;

import com.dAdK.dubAI.dto.ApiResponse;
import com.dAdK.dubAI.dto.TtsRequest;
import com.dAdK.dubAI.dto.audioanalysis.compressedaudio.CompressedAudio;
import com.dAdK.dubAI.dto.audioanalysis.compressedaudio.CompressionMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.info.MultimediaInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Unified service for audio compression and TTS processing
 * Handles both low-level compression and high-level TTS workflows
 */
@Service
public class AudioCompressionService {

    private static final Logger logger = LoggerFactory.getLogger(AudioCompressionService.class);

    private final TextToSpeechService textToSpeechService;

    public AudioCompressionService(TextToSpeechService textToSpeechService) {
        this.textToSpeechService = textToSpeechService;
    }


    /**
     * Generate and compress speech to Opus format
     * This is the main method controllers should use
     */
    public ApiResponse<CompressedAudio> generateCompressedSpeech(
            TtsRequest request,
            CompressionQuality quality) {

        logger.debug("Generating compressed speech: text='{}', quality={}", request.text(), quality);

        // Step 1: Generate audio from TTS
        byte[] audioData;
        try {
            audioData = textToSpeechService.generateSpeech(request);
        } catch (Exception e) {
            logger.error("Speech generation failed", e);
            return ApiResponse.error("Failed to generate speech: " + e.getMessage());
        }

        if (audioData == null || audioData.length == 0) {
            logger.error("Speech generation returned empty data");
            return ApiResponse.error("Speech generation returned no audio data");
        }

        // Step 2: Determine format and compress
        String voiceId = request.voiceType().getVoiceIdentifier();
        String inputFormat = voiceId.contains("Wavenet") ? "mp3" : "wav";
        int originalSize = audioData.length;

        logger.info("Generated audio: {} bytes, format: {}", originalSize, inputFormat);

        try {
            byte[] compressedAudio = compressToOpus(audioData, inputFormat, quality);

            CompressionMetadata metadata = CompressionMetadata.calculate(
                    originalSize, compressedAudio.length, quality.name(), quality.getBitrate(), "opus"
            );

            CompressedAudio result = CompressedAudio.builder()
                    .audioData(compressedAudio)
                    .metadata(metadata)
                    .build();

            return ApiResponse.success(result, "Audio compressed successfully");

        } catch (Exception e) {
            logger.error("Compression failed", e);
            return ApiResponse.error("Audio compression failed: " + e.getMessage());
        }
    }

    /**
     * Generate and compress speech to MP3 format
     */
    public ApiResponse<CompressedAudio> generateCompressedMP3(
            TtsRequest request,
            int bitrate) {

        logger.debug("Generating MP3 compressed speech: text='{}', bitrate={}kbps",
                request.text(), bitrate / 1000);

        // Step 1: Generate audio
        byte[] audioData;
        try {
            audioData = textToSpeechService.generateSpeech(request);
        } catch (Exception e) {
            logger.error("Speech generation failed", e);
            return ApiResponse.error("Failed to generate speech: " + e.getMessage());
        }

        if (audioData == null || audioData.length == 0) {
            return ApiResponse.error("Speech generation returned no audio data");
        }

        // Step 2: Compress to MP3
        try {
            String voiceId = request.voiceType().getVoiceIdentifier();
            String inputFormat = voiceId.contains("Wavenet") ? "mp3" : "wav";
            int originalSize = audioData.length;

            byte[] compressedAudio = compressToMP3(audioData, inputFormat, bitrate);

            CompressionMetadata metadata = CompressionMetadata.calculate(
                    originalSize, compressedAudio.length, "MP3", bitrate, "mp3"
            );

            CompressedAudio result = CompressedAudio.builder()
                    .audioData(compressedAudio)
                    .metadata(metadata)
                    .build();

            return ApiResponse.success(result, "Audio compressed to MP3 successfully");

        } catch (Exception e) {
            logger.error("MP3 compression failed", e);
            return ApiResponse.error("MP3 compression failed: " + e.getMessage());
        }
    }

    /**
     * Generate original (uncompressed) audio
     */
    public ApiResponse<CompressedAudio> generateOriginalAudio(TtsRequest request) {

        logger.debug("Generating original audio: text='{}'", request.text());

        try {
            byte[] audioData = textToSpeechService.generateSpeech(request);

            if (audioData == null || audioData.length == 0) {
                return ApiResponse.error("Failed to generate audio data");
            }

            String voiceId = request.voiceType().getVoiceIdentifier();
            String format = voiceId.contains("Wavenet") ? "mp3" : "wav";

            CompressionMetadata metadata = CompressionMetadata.builder()
                    .originalSize(audioData.length)
                    .compressedSize(audioData.length)
                    .compressionRatio(0.0)
                    .bytesSaved(0)
                    .quality("ORIGINAL")
                    .bitrate(0)
                    .format(format)
                    .build();

            CompressedAudio result = CompressedAudio.builder()
                    .audioData(audioData)
                    .metadata(metadata)
                    .build();

            return ApiResponse.success(result, "Original audio generated successfully");

        } catch (Exception e) {
            logger.error("Audio generation failed", e);
            return ApiResponse.error("Failed to generate audio: " + e.getMessage());
        }
    }

    // ============================================
    // LOW-LEVEL COMPRESSION METHODS
    // ============================================

    /**
     * Compress audio bytes to Opus format
     * Low-level method for direct compression
     */
    public byte[] compressToOpus(byte[] inputAudioData, String inputFormat, CompressionQuality quality)
            throws IOException {

        Path inputFile = null;
        Path outputFile = null;

        try {
            logger.info("Starting Opus compression: input size={}, format={}, quality={}",
                    inputAudioData.length, inputFormat, quality);

            // Create temporary files
            inputFile = Files.createTempFile("audio_input_", "." + inputFormat);
            outputFile = Files.createTempFile("audio_output_", ".opus");

            // Write input audio to temp file
            Files.write(inputFile, inputAudioData);

            // Verify input file can be read
            MultimediaObject inputObject = new MultimediaObject(inputFile.toFile());
            try {
                MultimediaInfo info = inputObject.getInfo();
                logger.info("Input audio info: duration={}ms, format={}",
                        info.getDuration(), info.getFormat());
            } catch (Exception e) {
                logger.warn("Could not read input audio info: {}", e.getMessage());
            }

            // Set up audio attributes for Opus
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("libopus");
            audio.setBitRate(quality.getBitrate());
            audio.setChannels(1); // Mono for voice
            audio.setSamplingRate(48000); // Opus standard

            // Set up encoding attributes
            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("opus");
            attrs.setAudioAttributes(audio);

            // Perform encoding
            Encoder encoder = new Encoder();
            encoder.encode(
                    new MultimediaObject(inputFile.toFile()),
                    outputFile.toFile(),
                    attrs
            );

            // Verify output
            if (!Files.exists(outputFile) || Files.size(outputFile) == 0) {
                throw new IOException("Output file was not created or is empty");
            }

            byte[] compressedAudio = Files.readAllBytes(outputFile);

            double compressionRatio = (1 - ((double) compressedAudio.length / inputAudioData.length)) * 100;
            logger.info("✓ Compression successful: {} bytes → {} bytes ({:.1f}% reduction) at {} kbps",
                    inputAudioData.length, compressedAudio.length, compressionRatio,
                    quality.getBitrate() / 1000);

            return compressedAudio;

        } catch (EncoderException e) {
            logger.error("✗ Audio encoding failed: {}", e.getMessage(), e);
            throw new IOException("Audio compression failed: " + e.getMessage(), e);
        } finally {
            cleanupTempFile(inputFile);
            cleanupTempFile(outputFile);
        }
    }

    /**
     * Compress audio bytes to MP3 format
     * Low-level method for direct compression
     */
    public byte[] compressToMP3(byte[] inputAudioData, String inputFormat, int bitrate)
            throws IOException {

        Path inputFile = null;
        Path outputFile = null;

        try {
            inputFile = Files.createTempFile("audio_input_", "." + inputFormat);
            outputFile = Files.createTempFile("audio_output_", ".mp3");

            Files.write(inputFile, inputAudioData);

            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("libmp3lame");
            audio.setBitRate(bitrate);
            audio.setChannels(1);
            audio.setSamplingRate(44100);

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("mp3");
            attrs.setAudioAttributes(audio);

            Encoder encoder = new Encoder();
            encoder.encode(
                    new MultimediaObject(inputFile.toFile()),
                    outputFile.toFile(),
                    attrs
            );

            byte[] compressedAudio = Files.readAllBytes(outputFile);

            logger.info("✓ MP3 compression: {} → {} bytes", inputAudioData.length, compressedAudio.length);

            return compressedAudio;

        } catch (EncoderException e) {
            logger.error("✗ MP3 encoding failed: {}", e.getMessage(), e);
            throw new IOException("MP3 compression failed: " + e.getMessage(), e);
        } finally {
            cleanupTempFile(inputFile);
            cleanupTempFile(outputFile);
        }
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Check if encoder is available
     */
    public boolean isEncoderAvailable() {
        try {
            new Encoder();
            return true;
        } catch (Exception e) {
            logger.error("Encoder not available", e);
            return false;
        }
    }

    /**
     * Clean up temporary file
     */
    private void cleanupTempFile(Path file) {
        if (file != null) {
            try {
                Files.deleteIfExists(file);
                logger.debug("Cleaned up temp file: {}", file);
            } catch (IOException e) {
                logger.warn("Failed to delete temp file: {}", file, e);
            }
        }
    }

    // ============================================
    // COMPRESSION QUALITY ENUM
    // ============================================

    public enum CompressionQuality {
        VOICE_LOW(24000),      // 24 kbps
        VOICE_MEDIUM(64000),   // 64 kbps
        VOICE_HIGH(96000),     // 96 kbps (recommended for TTS)
        MUSIC_MEDIUM(128000),  // 128 kbps
        MUSIC_HIGH(192000);    // 192 kbps

        private final int bitrate;

        CompressionQuality(int bitrate) {
            this.bitrate = bitrate;
        }

        public int getBitrate() {
            return bitrate;
        }
    }
}