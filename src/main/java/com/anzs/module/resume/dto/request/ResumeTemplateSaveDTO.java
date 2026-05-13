package com.anzs.module.resume.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResumeTemplateSaveDTO {
    @NotBlank(message = "模板名称不能为空")
    private String name;

    @NotNull(message = "模板类型不能为空")
    private Integer type;

    private String thumbnailUrl;

    @NotBlank(message = "结构定义不能为空")
    private String structureJson;

    @NotBlank(message = "模板Key不能为空")
    private String templateKey;

    private String templateVersion;

    private Integer status;
}
