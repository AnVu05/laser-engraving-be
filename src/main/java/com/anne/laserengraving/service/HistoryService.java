package com.anne.laserengraving.service;

import com.anne.laserengraving.dto.request.SaveHistoryRequest;
import com.anne.laserengraving.dto.response.HistoryItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HistoryService {
    Page<HistoryItemResponse> getHistory(Integer userId, String email, Pageable pageable);
    default Page<HistoryItemResponse> getHistory(Integer userId, Pageable pageable) {
        return getHistory(userId, null, pageable);
    }

    HistoryItemResponse getHistoryDetail(Long historyId, Integer userId, String email);
    default HistoryItemResponse getHistoryDetail(Long historyId, Integer userId) {
        return getHistoryDetail(historyId, userId, null);
    }

    HistoryItemResponse saveHistory(String email, Integer userId, SaveHistoryRequest request);

    void deleteHistory(Long historyId, Integer userId, String email);
    default void deleteHistory(Long historyId, Integer userId) {
        deleteHistory(historyId, userId, null);
    }
}
