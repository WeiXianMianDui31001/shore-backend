package com.anzs.module.community.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostVO {
    private Long id;
    private Long authorId;
    private String authorNickname;
    private String authorAvatar;
    private Integer authorRole;
    private String title;
    private String content;
    private String tags;
    private String images;
    private Integer scene;
    private Integer status;
    private Boolean isPinned;
    private Integer viewCount;
    private Integer likeCount;
    private Integer collectCount;
    private Integer endorseCount;
    private Boolean isExcellent;
    private Boolean liked;
    private Boolean collected;
    private Boolean endorsed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
