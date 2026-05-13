package com.anzs.module.resume.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ResumeVersionDetailVO extends ResumeVersionVO {
    private String contentJson;
}
