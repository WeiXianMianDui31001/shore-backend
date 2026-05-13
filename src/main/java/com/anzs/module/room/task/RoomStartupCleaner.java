package com.anzs.module.room.task;

import com.anzs.module.room.service.RoomRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomStartupCleaner implements ApplicationRunner {

    private final RoomRedisService roomRedisService;

    @Override
    public void run(ApplicationArguments args) {
        Set<Long> activeRooms = roomRedisService.getActiveRoomIds();
        if (!activeRooms.isEmpty()) {
            log.info("Cleaning up {} stale active room records from Redis after restart", activeRooms.size());
            for (Long roomId : activeRooms) {
                roomRedisService.clearRoom(roomId);
            }
            log.info("Redis room cleanup completed");
        }
    }
}
