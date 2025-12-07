package com.dAdK.dubAI.services.tts;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.sound.sampled.*;
import java.io.*;

@Service
public class AudioCompressionServiceOutdated {

    private static final Logger logger = LoggerFactory.getLogger(AudioCompressionServiceOutdated.class);

    // Configuration for compression - AGGRESSIVE settings for maximum compression
    private static final float TARGET_SAMPLE_RATE = 16000.0f; // Reduced from 44100 Hz (good for speech)
    private static final int TARGET_SAMPLE_SIZE = 8; // 8 bits instead of 16 (50% reduction)
    private static final int TARGET_CHANNELS = 1; // Mono instead of stereo
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;

    /**
     * Compresses WAV audio by reducing sample rate and converting to mono
     * Reduces size by approximately 50-75%
     */
    public byte[] compressWav(byte[] audioData) {
        ByteArrayInputStream bais = null;
        AudioInputStream originalStream = null;
        AudioInputStream convertedStream = null;

        try {
            bais = new ByteArrayInputStream(audioData);
            originalStream = AudioSystem.getAudioInputStream(bais);

            AudioFormat originalFormat = originalStream.getFormat();
            logger.info("Original format: {} Hz, {} channels, {} bits",
                    originalFormat.getSampleRate(),
                    originalFormat.getChannels(),
                    originalFormat.getSampleSizeInBits());

            // Define target format with lower quality but smaller size
            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    TARGET_SAMPLE_RATE,
                    TARGET_SAMPLE_SIZE,
                    TARGET_CHANNELS,
                    (TARGET_SAMPLE_SIZE / 8) * TARGET_CHANNELS, // frameSize
                    TARGET_SAMPLE_RATE, // frameRate
                    BIG_ENDIAN
            );

            // Check if conversion is supported
            if (!AudioSystem.isConversionSupported(targetFormat, originalFormat)) {
                logger.warn("Direct conversion not supported. Trying intermediate conversion...");

                // Try intermediate PCM conversion
                AudioFormat intermediateFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        originalFormat.getSampleRate(),
                        16,
                        TARGET_CHANNELS,
                        2 * TARGET_CHANNELS,
                        originalFormat.getSampleRate(),
                        false
                );

                if (AudioSystem.isConversionSupported(intermediateFormat, originalFormat)) {
                    AudioInputStream intermediateStream = AudioSystem.getAudioInputStream(intermediateFormat, originalStream);
                    originalStream.close();
                    originalStream = intermediateStream;
                    originalFormat = intermediateFormat;

                    if (!AudioSystem.isConversionSupported(targetFormat, originalFormat)) {
                        logger.warn("Conversion still not supported, returning original");
                        return audioData;
                    }
                } else {
                    logger.warn("Intermediate conversion not supported, returning original");
                    return audioData;
                }
            }

            // Convert to target format
            convertedStream = AudioSystem.getAudioInputStream(targetFormat, originalStream);

            // KEY FIX: Read all data into a byte array first, then create a new stream with known length
            ByteArrayOutputStream tempBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = convertedStream.read(buffer)) != -1) {
                tempBuffer.write(buffer, 0, bytesRead);
            }

            byte[] convertedBytes = tempBuffer.toByteArray();

            // Create a new AudioInputStream with known frame length
            ByteArrayInputStream convertedBais = new ByteArrayInputStream(convertedBytes);
            long frameLength = convertedBytes.length / targetFormat.getFrameSize();
            AudioInputStream finalStream = new AudioInputStream(
                    convertedBais,
                    targetFormat,
                    frameLength
            );

            // Now write to WAV format with known length
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            AudioSystem.write(finalStream, AudioFileFormat.Type.WAVE, baos);
            byte[] compressed = baos.toByteArray();

            finalStream.close();

            float compressionRatio = (1 - (float) compressed.length / audioData.length) * 100;
            logger.info(String.format("Compression achieved: %.2f%% size reduction (%d -> %d bytes)",
                    compressionRatio, audioData.length, compressed.length));

            return compressed;

        } catch (UnsupportedAudioFileException e) {
            logger.error("Unsupported audio file format: {}", e.getMessage());
            return audioData;
        } catch (IOException e) {
            logger.error("IO error during compression: {}", e.getMessage(), e);
            return audioData;
        } catch (IllegalArgumentException e) {
            logger.error("Invalid audio format parameters: {}", e.getMessage());
            return audioData;
        } finally {
            // Properly close all streams
            try {
                if (convertedStream != null) convertedStream.close();
                if (originalStream != null) originalStream.close();
                if (bais != null) bais.close();
            } catch (IOException e) {
                logger.error("Error closing streams: {}", e.getMessage());
            }
        }
    }

    /**
     * Optimizes MP3 by ensuring proper bitrate encoding
     * Note: MP3 is already compressed, but we can re-encode at lower bitrate if needed
     */
    public byte[] optimizeMp3(byte[] audioData) {
        // MP3 is already compressed format
        // For further compression, you'd need external libraries like LAME or JAVE
        logger.info("MP3 optimization: returning original (already compressed format)");
        logger.info("MP3 size: {} bytes", audioData.length);
        return audioData;
    }

    /**
     * Main method to compress audio based on format
     */
    public byte[] compressAudio(byte[] audioData, String format) {
        if (audioData == null || audioData.length == 0) {
            logger.warn("Audio data is null or empty");
            return audioData;
        }

        logger.info("Starting compression for format: {}, size: {} bytes", format, audioData.length);

        try {
            switch (format.toLowerCase()) {
                case "wav":
                case "audio/wav":
                    return compressWav(audioData);
                case "mp3":
                case "audio/mpeg":
                case "audio/mp3":
                    return optimizeMp3(audioData);
                default:
                    logger.warn("Unknown format: {}, returning original", format);
                    return audioData;
            }
        } catch (Exception e) {
            logger.error("Unexpected error during compression: {}", e.getMessage(), e);
            return audioData;
        }
    }

    /**
     * Converts audio to base64 with optional compression
     * This reduces transfer size significantly
     */
    public String toBase64Compressed(byte[] audioData, String format) {
        if (audioData == null || audioData.length == 0) {
            logger.warn("Cannot convert null or empty audio data to base64");
            return "";
        }

        byte[] compressed = compressAudio(audioData, format);
        String base64 = java.util.Base64.getEncoder().encodeToString(compressed);
        logger.info("Base64 encoded compressed audio: {} characters", base64.length());
        return base64;
    }

    /**
     * Validates if audio data appears to be a valid WAV file
     */
    public boolean isValidWav(byte[] audioData) {
        if (audioData == null || audioData.length < 44) {
            return false;
        }
        // Check for RIFF header
        return audioData[0] == 'R' && audioData[1] == 'I' &&
                audioData[2] == 'F' && audioData[3] == 'F';
    }

    /**
     * Validates if audio data appears to be a valid MP3 file
     */
    public boolean isValidMp3(byte[] audioData) {
        if (audioData == null || audioData.length < 3) {
            return false;
        }
        // Check for MP3 header (ID3 or frame sync)
        return (audioData[0] == 'I' && audioData[1] == 'D' && audioData[2] == '3') ||
                (audioData[0] == (byte) 0xFF && (audioData[1] & 0xE0) == 0xE0);
    }
}