package com.anzs.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommunityActionDTO {
    @NotBlank(message = "动作不能为空")
    private String action;
    private String reason;
}
