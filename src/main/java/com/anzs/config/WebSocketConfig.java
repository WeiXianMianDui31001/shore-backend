package com.anzs.config;

import com.anzs.common.util.JwtUtil;
import com.anzs.module.room.service.RoomService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final JwtUtil jwtUtil;
    private final RoomService roomService;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(roomWebSocketHandler(), "/ws/room/{roomId}")
                .addInterceptors(roomHandshakeInterceptor())
                .setAllowedOrigins("*");
    }

    public WebSocketHandler roomWebSocketHandler() {
        return new RoomWebSocketHandler(roomService);
    }

    public HandshakeInterceptor roomHandshakeInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                           WebSocketHandler wsHandler, Map<String, Object> attributes) {
                URI uri = request.getURI();
                String token = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");
                if (token != null) {
                    try {
                        Claims claims = jwtUtil.parseToken(token);
                        Long userId = Long.valueOf(claims.getSubject());
                        attributes.put("userId", userId);
                        return true;
                    } catch (Exception e) {
                        log.warn("WebSocket JWT validation failed");
                        return false;
                    }
                }
                return false;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Exception exception) {
            }
        };
    }

    public static class RoomWebSocketHandler extends TextWebSocketHandler {

        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RoomWebSocketHandler.class);

        private final RoomService roomService;
        // roomId -> sessions
        private final Map<Long, Map<String, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
        // roomId -> sequence counter
        private final Map<Long, AtomicLong> roomSequence = new ConcurrentHashMap<>();

        public RoomWebSocketHandler(RoomService roomService) {
            this.roomService = roomService;
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            Long roomId = extractRoomId(session);
            Long userId = (Long) session.getAttributes().get("userId");
            if (roomId == null || userId == null) {
                session.close();
                return;
            }
            roomSessions.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(session.getId(), session);
            roomSequence.putIfAbsent(roomId, new AtomicLong(System.currentTimeMillis()));
            log.info("User {} joined room {}", userId, roomId);
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            Long roomId = extractRoomId(session);
            Long userId = (Long) session.getAttributes().get("userId");
            if (roomId == null || userId == null) return;

            String payload = message.getPayload();
            // Simple JSON parsing using basic string operations for lightweight handling
            // In production, use Jackson ObjectMapper
            if (payload.contains("\"msgType\":\"CHAT\"") || payload.contains("\"msgType\": \"CHAT\"")) {
                handleChatMessage(roomId, userId, payload);
            } else if (payload.contains("\"msgType\":\"WHITEBOARD\"") || payload.contains("\"msgType\": \"WHITEBOARD\"")) {
                handleWhiteboardMessage(roomId, userId, payload);
            }
        }

        private void handleChatMessage(Long roomId, Long userId, String payload) {
            String content = extractJsonField(payload, "content");
            String clientMsgId = extractJsonField(payload, "clientMsgId");
            if (content == null || content.isEmpty()) return;

            var msg = roomService.saveChatMessage(roomId, userId, content, clientMsgId, 0);
            long seq = roomSequence.get(roomId).incrementAndGet();
            String broadcast = String.format(
                    "{\"msgType\":\"CHAT\",\"payload\":{\"sequenceNo\":%d,\"senderId\":%d,\"content\":\"%s\",\"createdAt\":\"%s\"}}",
                    seq, userId, escapeJson(content), msg.getCreatedAt()
            );
            broadcastToRoom(roomId, broadcast);
        }

        private void handleWhiteboardMessage(Long roomId, Long userId, String payload) {
            String opType = extractJsonField(payload, "opType");
            String opData = extractJsonObject(payload, "opData");
            if (opType == null) return;

            long seq = roomSequence.get(roomId).incrementAndGet();
            roomService.saveWhiteboardOp(roomId, userId, opType, opData, seq);
            String broadcast = String.format(
                    "{\"msgType\":\"WHITEBOARD\",\"payload\":{\"sequenceNo\":%d,\"userId\":%d,\"opType\":\"%s\",\"opData\":%s}}",
                    seq, userId, escapeJson(opType), opData == null ? "{}" : opData
            );
            broadcastToRoom(roomId, broadcast);
        }

        private void broadcastToRoom(Long roomId, String message) {
            Map<String, WebSocketSession> sessions = roomSessions.get(roomId);
            if (sessions == null) return;
            for (WebSocketSession s : sessions.values()) {
                if (s.isOpen()) {
                    try {
                        s.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        log.error("Broadcast error", e);
                    }
                }
            }
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            Long roomId = extractRoomId(session);
            if (roomId != null) {
                Map<String, WebSocketSession> sessions = roomSessions.get(roomId);
                if (sessions != null) sessions.remove(session.getId());
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

        private String extractJsonField(String json, String field) {
            String pattern = "\"" + field + "\"\\s*:\\s*\"";
            int idx = json.indexOf(pattern);
            if (idx == -1) {
                pattern = "\"" + field + "\" : \"";
                idx = json.indexOf(pattern);
            }
            if (idx == -1) return null;
            int start = idx + pattern.length();
            int end = json.indexOf('"', start);
            if (end == -1) return null;
            return json.substring(start, end);
        }

        private String extractJsonObject(String json, String field) {
            String pattern = "\"" + field + "\"\\s*:\\s*";
            int idx = json.indexOf(pattern);
            if (idx == -1) {
                pattern = "\"" + field + "\" : ";
                idx = json.indexOf(pattern);
            }
            if (idx == -1) return null;
            int start = idx + pattern.length();
            if (json.charAt(start) == '{') {
                int braceCount = 1;
                int i = start + 1;
                while (i < json.length() && braceCount > 0) {
                    char c = json.charAt(i);
                    if (c == '{') braceCount++;
                    else if (c == '}') braceCount--;
                    i++;
                }
                return json.substring(start, i);
            }
            return null;
        }

        private String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
    }
}
