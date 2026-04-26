package com.anzs.config;

import com.anzs.common.util.JwtUtil;
import com.anzs.module.room.websocket.RoomWebSocketHandler;
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
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final JwtUtil jwtUtil;
    private final RoomWebSocketHandler roomWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(roomWebSocketHandler, "/ws/room/{roomId}")
                .addInterceptors(roomHandshakeInterceptor())
                .setAllowedOrigins("*");
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
}
