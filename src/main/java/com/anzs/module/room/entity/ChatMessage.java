package com.anzs.module.room.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long roomId;
    private Long senderId;
    private Integer msgType;     // 0-文本 1-图片 2-文件 3-表情
    private String content;
    private String clientMsgId;
    private LocalDateTime createdAt;
}
