package com.anzs.module.room.websocket.handler;

import org.springframework.web.socket.WebSocketSession;

public interface WebSocketMessageHandler {

    boolean supports(String msgType);

    void handle(WebSocketSession session, Long roomId, Long userId, String payload);
}
