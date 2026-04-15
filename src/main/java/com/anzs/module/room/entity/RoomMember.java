package com.anzs.module.room.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("room_member")
public class RoomMember {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long roomId;
    private Long userId;
    private Integer role;        // 0-查看 1-评论 2-编辑
    private LocalDateTime joinedAt;
}
