package com.dAdK.dubAI.services.audioanalysisservice;

import com.dAdK.dubAI.dto.audioanalysis.AnalyzeAudioRequest;
import com.dAdK.dubAI.dto.audioanalysis.AnalyzeAudioResponse;

/**
 * Service interface for audio analysis operations.
 */
public interface AudioAnalysisService {

    /**
     * Analyzes an audio file to determine transcription, sentiment, tone, and pitch.
     *
     * @param request The audio analysis request containing base64 encoded audio data URI
     * @return AnalyzeAudioResponse containing the analysis results
     * @throws IllegalArgumentException if the audio data URI is invalid
     * @throws RuntimeException         if the analysis fails
     */
    AnalyzeAudioResponse analyzeAudio(AnalyzeAudioRequest request);
}
