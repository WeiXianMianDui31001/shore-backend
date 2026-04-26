package com.anzs.module.room.websocket;

import com.anzs.module.room.service.RoomRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomSessionManager {

    private final RoomRedisService roomRedisService;

    // roomId -> sessionId -> WebSocketSession
    private final Map<Long, Map<String, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    // roomId -> sequence counter (for both chat and whiteboard)
    private final Map<Long, AtomicLong> roomSequence = new ConcurrentHashMap<>();

    public void addSession(Long roomId, WebSocketSession session, Long userId) {
        roomSessions.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(session.getId(), session);
        roomSequence.putIfAbsent(roomId, new AtomicLong(System.currentTimeMillis()));
        roomRedisService.addMember(roomId, userId);
        roomRedisService.updateActivity(roomId);
        roomRedisService.markRoomActive(roomId);
        log.info("User {} joined room {}, online: {}", userId, roomId, getOnlineCount(roomId));
    }

    public void removeSession(Long roomId, WebSocketSession session, Long userId) {
        Map<String, WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            sessions.remove(session.getId());
            if (sessions.isEmpty()) {
                roomSessions.remove(roomId);
                roomSequence.remove(roomId);
            }
        }
        roomRedisService.removeMember(roomId, userId);
        roomRedisService.updateActivity(roomId);
        log.info("User {} left room {}, online: {}", userId, roomId, getOnlineCount(roomId));
    }

    public long nextSequence(Long roomId) {
        return roomSequence.computeIfAbsent(roomId, k -> new AtomicLong(System.currentTimeMillis())).incrementAndGet();
    }

    public void broadcast(Long roomId, String message) {
        Map<String, WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions == null) return;
        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession s : sessions.values()) {
            if (s.isOpen()) {
                try {
                    s.sendMessage(textMessage);
                } catch (IOException e) {
                    log.error("Broadcast error to session {}", s.getId(), e);
                }
            }
        }
    }

    public int getOnlineCount(Long roomId) {
        Map<String, WebSocketSession> sessions = roomSessions.get(roomId);
        return sessions == null ? 0 : sessions.size();
    }

    public Set<Long> getActiveRoomIds() {
        return Set.copyOf(roomSessions.keySet());
    }

    public boolean hasSessions(Long roomId) {
        Map<String, WebSocketSession> sessions = roomSessions.get(roomId);
        return sessions != null && !sessions.isEmpty();
    }
}
