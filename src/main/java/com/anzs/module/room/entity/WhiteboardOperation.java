package com.anzs.module.room.entity;

import com.anzs.config.PgJsonbTypeHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("whiteboard_operation")
public class WhiteboardOperation {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long roomId;
    private Long userId;
    private String opType;
    @TableField(typeHandler = PgJsonbTypeHandler.class)
    private String opData;
    private Long sequenceNo;
    private LocalDateTime createdAt;
}
