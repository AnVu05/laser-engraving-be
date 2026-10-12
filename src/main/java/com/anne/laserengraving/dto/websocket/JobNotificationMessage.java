package com.anne.laserengraving.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobNotificationMessage {
    private String event;          // "NEW_JOB_AVAILABLE"
    private Long jobId;
    private String jobCode;
    private String fileName;
    private Long fileSizeBytes;
    private String sha256Checksum;
    private String downloadUrl;
    private Double targetWidthMm;
    private Double targetHeightMm;
    private Integer feedRate;
    private Integer laserPower;
}
