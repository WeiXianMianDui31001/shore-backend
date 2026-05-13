package com.anzs.module.resume.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ResumeTemplateAdminVO extends ResumeTemplateVO {
    private String templateVersion;
    private Integer status;
}
