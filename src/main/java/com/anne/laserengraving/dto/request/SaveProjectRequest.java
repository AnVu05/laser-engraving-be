package com.anne.laserengraving.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveProjectRequest {

    private Long projectId;

    @NotBlank(message = "Tên dự án không được để trống")
    private String projectName;

    private Integer elementCount;

    @NotBlank(message = "Dữ liệu các đối tượng trên bàn khắc (elementsJson) không được để trống")
    private String elementsJson;

    private String parametersJson;

    private String thumbnailUrl;
}
