package com.anzs.module.resource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resource_audit")
public class ResourceAudit {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long resourceId;
    private Long adminId;
    private Integer action;      // 1-通过 2-驳回
    private String reason;
    private LocalDateTime createdAt;
}
