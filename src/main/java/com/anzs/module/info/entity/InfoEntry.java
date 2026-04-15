package com.anzs.module.info.entity;

import com.anzs.config.PgJsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("info_entry")
public class InfoEntry {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String title;
    private Integer scene;       // 0-考研 1-求职
    private String category;
    @TableField(typeHandler = PgJsonbTypeHandler.class)
    private String tags;
    private String sourceUrl;
    private String sourceName;
    private LocalDate updateTime;
    private Integer status;      // 0-上架 1-下架
    private Integer sortOrder;
    private Long adminId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
