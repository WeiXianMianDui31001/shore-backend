package com.anzs.module.resource.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResourceSubmitDTO {
    @NotBlank
    private String title;
    @NotBlank
    private String category;
    private String description;
    @NotBlank
    private String uploadId;  // 对应文件 URL
}
