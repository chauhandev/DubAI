package com.dAdK.dubAI.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class WavConverter {

    private static final Logger log = LoggerFactory.getLogger(WavConverter.class);

    // Audio format constants for Gemini TTS
    private static final int SAMPLE_RATE = 24000;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CHANNELS = 1;
    private static final int FRAME_SIZE = 2; // 2 bytes for 16-bit mono

    /**
     * Converts raw PCM audio data to WAV format using Java Sound API.
     * Gemini returns: 24000 Hz, 16-bit, mono, little-endian PCM
     */
    public static byte[] convertPcmToWav(byte[] pcmData) throws Exception {
        AudioFormat audioFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                SAMPLE_RATE,
                BITS_PER_SAMPLE,
                CHANNELS,
                FRAME_SIZE,
                SAMPLE_RATE,
                false // little-endian
        );

        ByteArrayInputStream bais = new ByteArrayInputStream(pcmData);
        AudioInputStream audioInputStream = new AudioInputStream(
                bais,
                audioFormat,
                pcmData.length / audioFormat.getFrameSize()
        );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, baos);
        audioInputStream.close();

        byte[] wavData = baos.toByteArray();
        log.debug("Converted {} bytes PCM to {} bytes WAV", pcmData.length, wavData.length);
        return wavData;
    }
}
