//package com.dAdK.dubAI.controller;
//
//import com.dAdK.dubAI.dto.TtsRequest;
//import com.dAdK.dubAI.enums.VoiceType;
//import com.dAdK.dubAI.services.tts.TextToSpeechService;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//class TTSControllerTest {
//
//    @Mock
//    private TextToSpeechService textToSpeechService;
//
//    @InjectMocks
//    private TextToSpeechController ttsController;
//
//    private static final String TEST_TEXT = "Hello, world!";
//    private static final byte[] MOCK_AUDIO_DATA = "mock audio data".getBytes();
//
//    @Test
//    void generateSpeech_wavenet_success() {
//        // Arrange
//        TtsRequest request = new TtsRequest(TEST_TEXT, VoiceType.EN_US_MALE);
//        when(textToSpeechService.generateSpeech(TEST_TEXT, VoiceType.EN_US_MALE)).thenReturn(MOCK_AUDIO_DATA);
//
//        // Act
//        ResponseEntity<?> response = ttsController.generateSpeech(request);
//
//        // Assert
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(MediaType.valueOf("audio/mpeg"), response.getHeaders().getContentType());
//        assertArrayEquals(MOCK_AUDIO_DATA, (byte[]) response.getBody());
//    }
//
//    @Test
//    void generateSpeech_gemini_success() {
//        // Arrange
//        TtsRequest request = new TtsRequest(TEST_TEXT, VoiceType.AOEDE);
//        when(textToSpeechService.generateSpeech(TEST_TEXT, VoiceType.AOEDE)).thenReturn(MOCK_AUDIO_DATA);
//
//        // Act
//        ResponseEntity<?> response = ttsController.generateSpeech(request);
//
//        // Assert
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(MediaType.valueOf("audio/wav"), response.getHeaders().getContentType());
//        assertArrayEquals(MOCK_AUDIO_DATA, (byte[]) response.getBody());
//    }
//
//    @Test
//    void generateSpeech_serviceReturnsNull_returnsInternalServerError() {
//        // Arrange
//        TtsRequest request = new TtsRequest(TEST_TEXT, VoiceType.EN_US_MALE);
//        when(textToSpeechService.generateSpeech(TEST_TEXT, VoiceType.EN_US_MALE)).thenReturn(null);
//
//        // Act
//        ResponseEntity<?> response = ttsController.generateSpeech(request);
//
//        // Assert
//        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
//        assertEquals("Failed to generate speech. Check server logs for details.", response.getBody());
//    }
//
//    @Test
//    void generateSpeech_serviceReturnsEmptyArray_returnsInternalServerError() {
//        // Arrange
//        TtsRequest request = new TtsRequest(TEST_TEXT, VoiceType.EN_US_MALE);
//        when(textToSpeechService.generateSpeech(TEST_TEXT, VoiceType.EN_US_MALE)).thenReturn(new byte[0]);
//
//        // Act
//        ResponseEntity<?> response = ttsController.generateSpeech(request);
//
//        // Assert
//        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
//        assertEquals("Failed to generate speech. Check server logs for details.", response.getBody());
//    }
//
//    @Test
//    void generateSpeech_serviceThrowsException_returnsInternalServerError() {
//        // Arrange
//        TtsRequest request = new TtsRequest(TEST_TEXT, VoiceType.EN_US_MALE);
//        when(textToSpeechService.generateSpeech(TEST_TEXT, VoiceType.EN_US_MALE)).thenThrow(new RuntimeException("Service failure"));
//
//        // Act & Assert
//        assertThrows(RuntimeException.class, () -> ttsController.generateSpeech(request));
//    }
//}
