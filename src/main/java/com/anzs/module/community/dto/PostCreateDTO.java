package com.anzs.module.community.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PostCreateDTO {
    @NotBlank(message = "标题不能为空")
    private String title;
    private String content;
    private String tags;
}
