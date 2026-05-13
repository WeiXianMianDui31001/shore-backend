package com.anzs.module.room.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageVO {
    private Long id;
    private Long roomId;
    private Long senderId;
    private String senderNickname;
    private String senderAvatar;
    private Integer msgType;
    private String content;
    private String clientMsgId;
    private Long sequenceNo;
    private LocalDateTime createdAt;
}
