package com.anzs.module.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AuditDTO {
    @NotNull(message = "审核动作不能为空")
    private Integer action;     // 1-通过 2-驳回
    private String reason;
}
