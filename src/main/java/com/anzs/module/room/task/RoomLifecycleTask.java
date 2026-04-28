package com.anzs.module.room.task;

import com.anzs.module.room.entity.DiscussionRoom;
import com.anzs.module.room.mapper.DiscussionRoomMapper;
import com.anzs.module.room.service.RoomArchiveService;
import com.anzs.module.room.service.RoomRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomLifecycleTask {

    private final RoomRedisService roomRedisService;
    private final RoomArchiveService roomArchiveService;
    private final DiscussionRoomMapper roomMapper;

    private static final long INACTIVE_THRESHOLD_MS = 5 * 60 * 1000; // 5 minutes

    @Scheduled(fixedRate = 30000)
    public void checkRooms() {
        Set<Long> activeRoomIds = roomRedisService.getActiveRoomIds();
        if (activeRoomIds.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        long currentMillis = System.currentTimeMillis();

        for (Long roomId : activeRoomIds) {
            try {
                DiscussionRoom room = roomMapper.selectById(roomId);
                if (room == null || room.getStatus() != 0) {
                    roomRedisService.clearRoom(roomId);
                    continue;
                }

                // 1. 检查是否到达过期时间
                if (room.getExpireAt() != null && room.getExpireAt().isBefore(now)) {
                    log.info("Room {} expired at {}, archiving", roomId, room.getExpireAt());
                    roomArchiveService.archiveRoom(roomId);
                    continue;
                }

                // 2. 检查是否长时间无人（房间内无 WebSocket 连接且超过 5 分钟无活动）
                Long lastActivity = roomRedisService.getLastActivity(roomId);
                if (lastActivity != null) {
                    long inactiveMillis = currentMillis - lastActivity;
                    if (inactiveMillis > INACTIVE_THRESHOLD_MS) {
                        log.info("Room {} inactive for {}ms, archiving", roomId, inactiveMillis);
                        roomArchiveService.archiveRoom(roomId);
                    }
                }
            } catch (Exception e) {
                log.error("Error checking room {}", roomId, e);
            }
        }
    }
}
