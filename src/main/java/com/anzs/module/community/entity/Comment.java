package com.anzs.module.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long postId;
    private Long parentId;
    private Long authorId;
    private String content;
    private Integer status;      // 0-正常 1-已删除
    private LocalDateTime createdAt;
}
