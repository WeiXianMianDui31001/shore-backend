package com.anzs.module.resume.entity;

import com.anzs.config.PgJsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resume")
public class Resume {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long templateId;
    private String title;
    @TableField(typeHandler = PgJsonbTypeHandler.class)
    private String contentJson;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
