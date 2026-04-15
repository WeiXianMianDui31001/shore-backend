package com.anzs.module.resume.entity;

import com.anzs.config.PgJsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resume_template")
public class ResumeTemplate {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private Integer type;        // 0-考研 1-求职
    private String thumbnailUrl;
    @TableField(typeHandler = PgJsonbTypeHandler.class)
    private String structureJson;
    private Integer status;      // 0-启用 1-禁用
    private LocalDateTime createdAt;
}
