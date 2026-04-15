package com.anzs.module.admin.entity;

import com.anzs.config.PgJsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_log")
public class AdminLog {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long adminId;
    private String action;
    private String targetType;
    private Long targetId;
    @TableField(typeHandler = PgJsonbTypeHandler.class)
    private String detail;
    private String ip;
    private LocalDateTime createdAt;
}
