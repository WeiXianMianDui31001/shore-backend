package com.anzs.module.resume.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResumePreviewHtmlDTO {
    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    @NotBlank(message = "简历内容不能为空")
    @Size(max = 400_000, message = "内容过长")
    private String contentJson;
}
