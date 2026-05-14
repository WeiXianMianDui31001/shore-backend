package com.anzs.module.resume.entity;

import com.anzs.config.PgJsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resume_version")
public class ResumeVersion {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long resumeId;
    private Integer versionNo;
    private Long templateId;
    @TableField(typeHandler = PgJsonbTypeHandler.class)
    private String contentJson;
    private LocalDateTime createdAt;
}
