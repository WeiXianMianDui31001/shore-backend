package com.anzs.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("points_rule")
public class PointsRule {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Integer version;
    private Integer downloadCost;
    private Integer uploadReward;
    private BigDecimal shareRatio;
    private Integer dailyLimit;
    private LocalDateTime effectiveTime;
    private Boolean isActive;
    private String changeNote;
    private Long createdBy;
    private LocalDateTime createdAt;
}
