package com.anzs.module.room.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketMessageRouter messageRouter;
    private final RoomSessionManager sessionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long roomId = extractRoomId(session);
        Long userId = (Long) session.getAttributes().get("userId");
        if (roomId == null || userId == null) {
            session.close();
            return;
        }
        sessionManager.addSession(roomId, session, userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long roomId = extractRoomId(session);
        Long userId = (Long) session.getAttributes().get("userId");
        if (roomId == null || userId == null) return;

        messageRouter.route(session, roomId, userId, message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long roomId = extractRoomId(session);
        Long userId = (Long) session.getAttributes().get("userId");
        if (roomId != null && userId != null) {
            sessionManager.removeSession(roomId, session, userId);
        }
    }

    private Long extractRoomId(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        if (parts.length > 0) {
            try {
                return Long.valueOf(parts[parts.length - 1]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
