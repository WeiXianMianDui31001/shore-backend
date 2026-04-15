package com.anzs.module.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Integer type;        // 0-系统 1-互动 2-积分
    private String title;
    private String content;
    private Long bizId;
    private String bizType;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
