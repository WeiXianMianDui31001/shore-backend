package com.anzs.module.resume.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResumeSaveDTO {
    @NotBlank(message = "简历标题不能为空")
    private String title;

    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    @NotBlank(message = "简历内容不能为空")
    private String contentJson;
}
