package com.anne.laserengraving.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobProgressMessage {
    private String type;               // "PROGRESS_UPDATE"
    private String deviceCode;         // "LASER-HN-001"
    private String jobCode;            // "JOB-20261012-001"
    private String status;             // "PRINTING", "COMPLETED"
    private Double progressPercent;    // 35.0, 60.0, 100.0%
    private Integer currentLine;       // Dòng G-code đang khắc
    private Integer totalLines;        // Tổng số dòng G-code
    private Integer remainingSeconds;  // Thời gian ước tính còn lại (ETA)
    private Long timestamp;
}
