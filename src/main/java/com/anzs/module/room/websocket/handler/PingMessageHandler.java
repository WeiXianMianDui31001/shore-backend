package com.anzs.module.room.websocket.handler;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Slf4j
@Component
public class PingMessageHandler implements WebSocketMessageHandler {

    @Override
    public boolean supports(String msgType) {
        return "PING".equals(msgType);
    }

    @Override
    public void handle(WebSocketSession session, Long roomId, Long userId, String payload) {
        JSONObject pong = new JSONObject();
        pong.set("msgType", "PONG");
        pong.set("timestamp", System.currentTimeMillis());
        try {
            session.sendMessage(new TextMessage(pong.toString()));
        } catch (IOException e) {
            log.warn("Failed to send PONG to session {}", session.getId(), e);
        }
    }
}
