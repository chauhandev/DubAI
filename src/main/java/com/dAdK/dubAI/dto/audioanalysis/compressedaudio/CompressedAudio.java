package com.dAdK.dubAI.dto.audioanalysis.compressedaudio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Audio compression result with metadata
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompressedAudio {
    private byte[] audioData;
    private CompressionMetadata metadata;

    public Map<String, String> getHeaderMap() {
        return metadata != null ? metadata.toHeaderMap() : Map.of();
    }

}