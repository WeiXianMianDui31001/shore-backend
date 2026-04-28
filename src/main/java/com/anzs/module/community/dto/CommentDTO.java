package com.anzs.module.community.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentDTO {
    private Long parentId;
    private String content;
    private java.util.List<String> images;
}
