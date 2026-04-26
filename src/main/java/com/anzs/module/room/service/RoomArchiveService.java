package com.anzs.module.room.service;

import com.anzs.module.room.entity.ChatMessage;
import com.anzs.module.room.entity.DiscussionRoom;
import com.anzs.module.room.entity.WhiteboardOperation;
import com.anzs.module.room.mapper.ChatMessageMapper;
import com.anzs.module.room.mapper.DiscussionRoomMapper;
import com.anzs.module.room.mapper.WhiteboardOperationMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomArchiveService {

    private final RoomRedisService roomRedisService;
    private final ChatMessageMapper chatMessageMapper;
    private final WhiteboardOperationMapper whiteboardOperationMapper;
    private final DiscussionRoomMapper discussionRoomMapper;

    @Transactional
    public void archiveRoom(Long roomId) {
        log.info("Archiving room {}", roomId);

        List<ChatMessage> messages = roomRedisService.getAllChatMessages(roomId);
        List<WhiteboardOperation> ops = roomRedisService.getAllWhiteboardOps(roomId);
        String snapshot = roomRedisService.getWhiteboardSnapshot(roomId);

        if (!messages.isEmpty()) {
            for (ChatMessage msg : messages) {
                chatMessageMapper.insert(msg);
            }
            log.info("Archived {} chat messages for room {}", messages.size(), roomId);
        }

        if (!ops.isEmpty()) {
            for (WhiteboardOperation op : ops) {
                whiteboardOperationMapper.insert(op);
            }
            log.info("Archived {} whiteboard ops for room {}", ops.size(), roomId);
        }

        LocalDateTime now = LocalDateTime.now();
        discussionRoomMapper.update(null, new LambdaUpdateWrapper<DiscussionRoom>()
                .eq(DiscussionRoom::getId, roomId)
                .set(DiscussionRoom::getStatus, 1)
                .set(DiscussionRoom::getClosedAt, now)
                .set(DiscussionRoom::getUpdatedAt, now)
                .set(snapshot != null, DiscussionRoom::getWhiteboardSnapshot, snapshot));

        roomRedisService.clearRoom(roomId);
        log.info("Room {} archived successfully", roomId);
    }
}
