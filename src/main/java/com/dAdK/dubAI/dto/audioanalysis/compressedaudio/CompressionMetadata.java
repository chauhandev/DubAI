package com.dAdK.dubAI.dto.audioanalysis.compressedaudio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompressionMetadata {
    private int originalSize;
    private int compressedSize;
    private double compressionRatio;
    private int bytesSaved;
    private String quality;
    private int bitrate;
    private String format;

    public Map<String, String> toHeaderMap() {
        return Map.of(
                "X-Original-Size", String.valueOf(originalSize),
                "X-Compressed-Size", String.valueOf(compressedSize),
                "X-Compression-Ratio", String.format("%.1f%%", compressionRatio),
                "X-Bytes-Saved", String.valueOf(bytesSaved),
                "X-Compression-Quality", quality,
                "X-Bitrate", bitrate / 1000 + "kbps",
                "X-Format", format
        );
    }

    public static CompressionMetadata calculate(int originalSize, int compressedSize,
                                                String quality, int bitrate, String format) {
        return CompressionMetadata.builder()
                .originalSize(originalSize)
                .compressedSize(compressedSize)
                .compressionRatio((1 - (double) compressedSize / originalSize) * 100)
                .bytesSaved(originalSize - compressedSize)
                .quality(quality)
                .bitrate(bitrate)
                .format(format)
                .build();
    }
}