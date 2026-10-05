package com.anne.laserengraving.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GcodeGenerateResponse {
    private Long historyId;
    private String filename;
    private Long fileSizeBytes;
    private Integer totalLines;
    private Double outputWidthMm;
    private Double outputHeightMm;
    private Integer estimatedTimeSeconds;
    private String downloadUrl;
    private List<String> gcodePreviewSnippet;
    private String fullGcodeContent;
}
