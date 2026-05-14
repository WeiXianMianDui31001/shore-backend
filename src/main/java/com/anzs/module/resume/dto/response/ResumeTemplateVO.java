package com.anzs.module.resume.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResumeTemplateVO {
    private Long id;
    private String name;
    private Integer type;
    private String thumbnailUrl;
    private String structureJson;
    private String templateKey;
    private LocalDateTime createdAt;
}
