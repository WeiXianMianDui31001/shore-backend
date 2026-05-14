package com.anzs.module.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PointsRuleUpdateDTO {
    @NotNull(message = "下载消耗积分不能为空")
    private Integer downloadCost;
    @NotNull(message = "上传奖励积分不能为空")
    private Integer uploadReward;
    private BigDecimal shareRatio;
    @NotNull(message = "每日上限不能为空")
    private Integer dailyLimit;
    private String changeNote;
}
