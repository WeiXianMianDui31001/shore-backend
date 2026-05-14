package com.anzs.module.resume.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResumeVO {
    /** JS Number 无法安全表示雪花 id，序列化为字符串避免列表/路由中的精度丢失 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private Long templateId;
    private String templateName;
    private String title;
    private Integer version;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long currentVersionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
