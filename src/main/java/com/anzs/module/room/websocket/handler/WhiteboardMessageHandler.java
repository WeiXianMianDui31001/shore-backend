package com.anzs.module.room.websocket.handler;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.anzs.module.room.entity.WhiteboardOperation;
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
public class WhiteboardMessageHandler implements WebSocketMessageHandler {

    private final RoomRedisService roomRedisService;
    private final RoomSessionManager sessionManager;

    @Override
    public boolean supports(String msgType) {
        return "WHITEBOARD".equals(msgType);
    }

    @Override
    public void handle(WebSocketSession session, Long roomId, Long userId, String payload) {
        JSONObject json = JSONUtil.parseObj(payload);
        String opType = json.getStr("opType");
        String snapshot = json.getStr("snapshot");
        JSONObject opData = json.getJSONObject("opData");

        if (opType == null || opType.isBlank()) return;

        long seq = sessionManager.nextSequence(roomId);
        WhiteboardOperation op = new WhiteboardOperation();
        op.setRoomId(roomId);
        op.setUserId(userId);
        op.setOpType(opType);
        op.setOpData(opData != null ? opData.toString() : "{}");
        op.setSequenceNo(seq);
        op.setCreatedAt(LocalDateTime.now());

        roomRedisService.pushWhiteboardOp(roomId, op);
        roomRedisService.updateActivity(roomId);

        if (snapshot != null && !snapshot.isBlank()) {
            roomRedisService.setWhiteboardSnapshot(roomId, snapshot);
        }

        JSONObject broadcast = new JSONObject();
        broadcast.set("msgType", "WHITEBOARD");
        JSONObject data = new JSONObject();
        data.set("sequenceNo", seq);
        data.set("userId", userId);
        data.set("opType", opType);
        if (opData != null) data.set("opData", opData);
        broadcast.set("payload", data);

        sessionManager.broadcast(roomId, broadcast.toString());
    }
}
