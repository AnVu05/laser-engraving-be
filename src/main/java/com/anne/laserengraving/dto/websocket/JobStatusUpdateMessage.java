package com.anne.laserengraving.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobStatusUpdateMessage {
    private String type;          // "JOB_STATUS_CHANGE"
    private String deviceCode;    // "LASER-HN-001"
    private String jobCode;       // "JOB-20261012-001"
    private String status;        // "DOWNLOADING", "AWAITING_ONSITE_CONFIRMATION", "PRINTING", "COMPLETED", "FAILED"
    private String message;
    private Long timestamp;
}
