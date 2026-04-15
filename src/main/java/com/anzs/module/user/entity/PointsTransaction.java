package com.anzs.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("points_transaction")
public class PointsTransaction {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Integer type;        // 1-收入 2-支出
    private Integer amount;
    private Integer balanceAfter;
    private String sourceType;
    private Long bizId;
    private Integer ruleVersion;
    private LocalDateTime createdAt;
}
