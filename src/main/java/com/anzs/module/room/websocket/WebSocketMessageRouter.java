package com.anzs.module.room.websocket;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.anzs.module.room.websocket.handler.WebSocketMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketMessageRouter {

    private final List<WebSocketMessageHandler> handlers;

    public void route(WebSocketSession session, Long roomId, Long userId, String payload) {
        try {
            JSONObject json = JSONUtil.parseObj(payload);
            String msgType = json.getStr("msgType");
            if (msgType == null) {
                log.warn("Missing msgType in websocket payload");
                return;
            }

            for (WebSocketMessageHandler handler : handlers) {
                if (handler.supports(msgType)) {
                    handler.handle(session, roomId, userId, payload);
                    return;
                }
            }

            log.warn("No handler found for msgType: {}", msgType);
        } catch (Exception e) {
            log.error("Failed to route websocket message", e);
        }
    }
}
