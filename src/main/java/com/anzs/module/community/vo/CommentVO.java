package com.anzs.module.community.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentVO {
    private Long id;
    private Long postId;
    private Long parentId;
    private Long authorId;
    private String authorNickname;
    private String authorAvatar;
    private Integer authorRole;
    private String content;
    private String images;
    private Integer status;
    private LocalDateTime createdAt;
}
