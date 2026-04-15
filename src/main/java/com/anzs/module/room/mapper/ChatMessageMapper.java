package com.anzs.module.room.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.anzs.module.room.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
