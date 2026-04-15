package com.anzs.module.community.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentDTO {
    private Long parentId;
    @NotBlank(message = "内容不能为空")
    private String content;
}
