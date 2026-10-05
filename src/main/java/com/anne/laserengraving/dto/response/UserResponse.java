package com.anne.laserengraving.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Integer userId;
    private String username;
    private String fullName;
    private String email;
    private String avatarUrl;
    private String authProvider;
    private String role;
    private LocalDateTime createdAt;
}
