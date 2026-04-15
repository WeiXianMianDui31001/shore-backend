package com.anzs.module.community.entity;

import com.anzs.config.PgJsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("post")
public class Post {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long authorId;
    private String title;
    private String content;
    @TableField(typeHandler = PgJsonbTypeHandler.class)
    private String tags;
    private Integer status;      // 0-正常 1-已解决 2-已下架
    private Boolean isPinned;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
