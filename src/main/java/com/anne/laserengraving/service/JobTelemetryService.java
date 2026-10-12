package com.anne.laserengraving.service;

import com.anne.laserengraving.dto.websocket.JobProgressMessage;
import com.anne.laserengraving.dto.websocket.JobStatusUpdateMessage;

public interface JobTelemetryService {

    void processProgressUpdate(JobProgressMessage progress);

    void processStatusUpdate(JobStatusUpdateMessage statusUpdate);
}
