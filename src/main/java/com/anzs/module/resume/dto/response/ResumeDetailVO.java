package com.anzs.module.resume.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ResumeDetailVO extends ResumeVO {
    private String contentJson;
}
