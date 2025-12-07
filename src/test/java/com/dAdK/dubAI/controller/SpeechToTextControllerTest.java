package com.dAdK.dubAI.controller;

import com.dAdK.dubAI.dto.audioanalysis.AnalyzeAudioResponse;
import com.dAdK.dubAI.services.audioanalysisservice.AudioAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import com.dAdK.dubAI.config.security.JwtAuthFilter;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SpeechToTextController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
public class SpeechToTextControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AudioAnalysisService audioAnalysisService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    public void analyzeAudio_whenValidFile_shouldReturnOk() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "audioFile",
                "test.mp3",
                "audio/mpeg",
                "test audio data".getBytes()
        );

        AnalyzeAudioResponse mockResponse = new AnalyzeAudioResponse();
        // Populate mockResponse with some data if necessary

        when(audioAnalysisService.analyzeAudio(any())).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(multipart("/api/v1/audio/analyze").file(file))
                .andExpect(status().isOk());
    }

    @Test
    public void analyzeAudio_whenFileIsEmpty_shouldReturnBadRequest() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "audioFile",
                "test.mp3",
                "audio/mpeg",
                new byte[0]
        );

        // When & Then
        mockMvc.perform(multipart("/api/v1/audio/analyze").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void analyzeAudio_whenFileIsTooLarge_shouldReturnBadRequest() throws Exception {
        // Given
        byte[] largeFileContent = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile file = new MockMultipartFile(
                "audioFile",
                "largefile.mp3",
                "audio/mpeg",
                largeFileContent
        );

        // When & Then
        mockMvc.perform(multipart("/api/v1/audio/analyze").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void analyzeAudio_whenInvalidFileType_shouldReturnBadRequest() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "audioFile",
                "test.txt",
                "text/plain",
                "this is not an audio file".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/v1/audio/analyze").file(file))
                .andExpect(status().isBadRequest());
    }
}
