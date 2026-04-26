package com.anzs.module.room.entity;

import com.anzs.config.PgJsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("discussion_room")
public class DiscussionRoom {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long creatorId;
    private String name;
    private String passwordHash;
    private Integer maxMembers;
    private LocalDateTime expireAt;
    private LocalDateTime closedAt;
    private Integer status;      // 0-开启 1-已关闭
    @TableField(typeHandler = PgJsonbTypeHandler.class)
    private String whiteboardSnapshot;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
