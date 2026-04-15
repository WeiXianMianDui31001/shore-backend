package com.anzs.module.resource.entity;

import com.anzs.config.PgJsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resource")
public class Resource {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long uploaderId;
    private String title;
    private String category;
    @TableField(typeHandler = PgJsonbTypeHandler.class)
    private String tags;
    private String description;
    private String fileUrl;
    private Long fileSize;
    private String fileType;
    private Integer status;      // 0-待审核 1-已通过 2-已驳回
    private Integer pointsCost;
    private Integer downloadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
