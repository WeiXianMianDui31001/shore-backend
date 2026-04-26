package com.anzs.module.room.service;

import cn.hutool.json.JSONUtil;
import com.anzs.infrastructure.cache.RedisCache;
import com.anzs.module.room.entity.ChatMessage;
import com.anzs.module.room.entity.WhiteboardOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomRedisService {

    private final RedisCache redisCache;

    private static final String KEY_ROOM_ACTIVE = "room:active";
    private static final String KEY_CHAT = "room:%d:chat";
    private static final String KEY_WB_OPS = "room:%d:wb:ops";
    private static final String KEY_WB_SNAPSHOT = "room:%d:wb:snapshot";
    private static final String KEY_MEMBERS = "room:%d:members";
    private static final String KEY_ACTIVITY = "room:%d:activity";
    private static final String KEY_INFO = "room:%d:info";

    // ===== 房间活跃状态 =====

    public void markRoomActive(Long roomId) {
        redisCache.addSet(KEY_ROOM_ACTIVE, roomId.toString());
    }

    public void unmarkRoomActive(Long roomId) {
        redisCache.removeSet(KEY_ROOM_ACTIVE, roomId.toString());
    }

    public Set<Long> getActiveRoomIds() {
        Set<String> ids = redisCache.setMembers(KEY_ROOM_ACTIVE);
        if (ids == null) return Collections.emptySet();
        return ids.stream().map(Long::valueOf).collect(Collectors.toSet());
    }

    public boolean isRoomActive(Long roomId) {
        Set<String> activeRooms = redisCache.setMembers(KEY_ROOM_ACTIVE);
        return activeRooms != null && activeRooms.contains(roomId.toString());
    }

    // ===== 聊天消息 =====

    public void pushChatMessage(Long roomId, ChatMessage msg) {
        redisCache.pushList(KEY_CHAT.formatted(roomId), JSONUtil.toJsonStr(msg));
    }

    public List<ChatMessage> getChatMessages(Long roomId, int start, int end) {
        List<String> list = redisCache.rangeList(KEY_CHAT.formatted(roomId), start, end);
        if (list == null) return Collections.emptyList();
        return list.stream()
                .map(s -> JSONUtil.toBean(s, ChatMessage.class))
                .collect(Collectors.toList());
    }

    public List<ChatMessage> getAllChatMessages(Long roomId) {
        Long size = redisCache.listSize(KEY_CHAT.formatted(roomId));
        if (size == null || size == 0) return Collections.emptyList();
        return getChatMessages(roomId, 0, size.intValue() - 1);
    }

    public void deleteChatMessages(Long roomId) {
        redisCache.delete(KEY_CHAT.formatted(roomId));
    }

    // ===== 白板操作 =====

    public void pushWhiteboardOp(Long roomId, WhiteboardOperation op) {
        redisCache.pushList(KEY_WB_OPS.formatted(roomId), JSONUtil.toJsonStr(op));
    }

    public List<WhiteboardOperation> getWhiteboardOps(Long roomId, int start, int end) {
        List<String> list = redisCache.rangeList(KEY_WB_OPS.formatted(roomId), start, end);
        if (list == null) return Collections.emptyList();
        return list.stream()
                .map(s -> JSONUtil.toBean(s, WhiteboardOperation.class))
                .collect(Collectors.toList());
    }

    public List<WhiteboardOperation> getAllWhiteboardOps(Long roomId) {
        Long size = redisCache.listSize(KEY_WB_OPS.formatted(roomId));
        if (size == null || size == 0) return Collections.emptyList();
        return getWhiteboardOps(roomId, 0, size.intValue() - 1);
    }

    public void deleteWhiteboardOps(Long roomId) {
        redisCache.delete(KEY_WB_OPS.formatted(roomId));
    }

    // ===== 白板快照 =====

    public void setWhiteboardSnapshot(Long roomId, String base64Snapshot) {
        redisCache.set(KEY_WB_SNAPSHOT.formatted(roomId), base64Snapshot, 7, TimeUnit.DAYS);
    }

    public String getWhiteboardSnapshot(Long roomId) {
        return redisCache.get(KEY_WB_SNAPSHOT.formatted(roomId));
    }

    public void deleteWhiteboardSnapshot(Long roomId) {
        redisCache.delete(KEY_WB_SNAPSHOT.formatted(roomId));
    }

    // ===== 在线成员 =====

    public void addMember(Long roomId, Long userId) {
        redisCache.addSet(KEY_MEMBERS.formatted(roomId), userId.toString());
    }

    public void removeMember(Long roomId, Long userId) {
        redisCache.removeSet(KEY_MEMBERS.formatted(roomId), userId.toString());
    }

    public Set<Long> getMembers(Long roomId) {
        Set<String> members = redisCache.setMembers(KEY_MEMBERS.formatted(roomId));
        if (members == null) return Collections.emptySet();
        return members.stream().map(Long::valueOf).collect(Collectors.toSet());
    }

    public int getOnlineCount(Long roomId) {
        Long size = redisCache.setSize(KEY_MEMBERS.formatted(roomId));
        return size == null ? 0 : size.intValue();
    }

    public void clearMembers(Long roomId) {
        redisCache.delete(KEY_MEMBERS.formatted(roomId));
    }

    // ===== 活跃度 =====

    public void updateActivity(Long roomId) {
        redisCache.set(KEY_ACTIVITY.formatted(roomId), String.valueOf(Instant.now().toEpochMilli()));
    }

    public Long getLastActivity(Long roomId) {
        String val = redisCache.get(KEY_ACTIVITY.formatted(roomId));
        return val == null ? null : Long.valueOf(val);
    }

    public void deleteActivity(Long roomId) {
        redisCache.delete(KEY_ACTIVITY.formatted(roomId));
    }

    // ===== 房间元数据 =====

    public void setRoomInfo(Long roomId, String field, String value) {
        redisCache.setHash(KEY_INFO.formatted(roomId), field, value);
    }

    public String getRoomInfo(Long roomId, String field) {
        return redisCache.getHash(KEY_INFO.formatted(roomId), field);
    }

    public void deleteRoomInfo(Long roomId) {
        redisCache.delete(KEY_INFO.formatted(roomId));
    }

    // ===== 清理整个房间 =====

    public void clearRoom(Long roomId) {
        deleteChatMessages(roomId);
        deleteWhiteboardOps(roomId);
        deleteWhiteboardSnapshot(roomId);
        clearMembers(roomId);
        deleteActivity(roomId);
        deleteRoomInfo(roomId);
        unmarkRoomActive(roomId);
    }
}
