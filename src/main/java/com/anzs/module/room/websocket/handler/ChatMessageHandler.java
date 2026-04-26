package com.anzs.module.room.websocket.handler;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.anzs.module.room.entity.ChatMessage;
import com.anzs.module.room.service.RoomRedisService;
import com.anzs.module.room.websocket.RoomSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageHandler implements WebSocketMessageHandler {

    private final RoomRedisService roomRedisService;
    private final RoomSessionManager sessionManager;

    @Override
    public boolean supports(String msgType) {
        return "CHAT".equals(msgType);
    }

    @Override
    public void handle(WebSocketSession session, Long roomId, Long userId, String payload) {
        JSONObject json = JSONUtil.parseObj(payload);
        String content = json.getStr("content");
        String clientMsgId = json.getStr("clientMsgId");

        if (content == null || content.isBlank()) return;

        long seq = sessionManager.nextSequence(roomId);
        ChatMessage msg = new ChatMessage();
        msg.setRoomId(roomId);
        msg.setSenderId(userId);
        msg.setMsgType(0);
        msg.setContent(content);
        msg.setClientMsgId(clientMsgId);
        msg.setSequenceNo(seq);
        msg.setCreatedAt(LocalDateTime.now());

        roomRedisService.pushChatMessage(roomId, msg);
        roomRedisService.updateActivity(roomId);

        JSONObject broadcast = new JSONObject();
        broadcast.set("msgType", "CHAT");
        JSONObject data = new JSONObject();
        data.set("sequenceNo", seq);
        data.set("senderId", userId);
        data.set("content", content);
        data.set("createdAt", msg.getCreatedAt().toString());
        if (clientMsgId != null) data.set("clientMsgId", clientMsgId);
        broadcast.set("payload", data);

        sessionManager.broadcast(roomId, broadcast.toString());
    }
}
