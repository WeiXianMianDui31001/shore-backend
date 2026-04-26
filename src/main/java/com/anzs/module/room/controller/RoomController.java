package com.anzs.module.room.controller;

import com.anzs.common.Result;
import com.anzs.config.security.SecurityUser;
import com.anzs.module.room.dto.RoomCreateDTO;
import com.anzs.module.room.dto.RoomJoinDTO;
import com.anzs.module.room.entity.ChatMessage;
import com.anzs.module.room.entity.DiscussionRoom;
import com.anzs.module.room.entity.WhiteboardOperation;
import com.anzs.module.room.service.RoomRedisService;
import com.anzs.module.room.service.RoomService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final RoomRedisService roomRedisService;

    @GetMapping
    public Result<IPage<Map<String, Object>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        IPage<DiscussionRoom> p = roomService.roomList(keyword, page, size);
        List<Map<String, Object>> records = p.getRecords().stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("creatorId", r.getCreatorId());
            map.put("name", r.getName());
            map.put("maxMembers", r.getMaxMembers());
            map.put("expireAt", r.getExpireAt());
            map.put("status", r.getStatus());
            map.put("whiteboardSnapshot", r.getWhiteboardSnapshot());
            map.put("createdAt", r.getCreatedAt());
            map.put("updatedAt", r.getUpdatedAt());
            map.put("onlineCount", roomRedisService.getOnlineCount(r.getId()));
            map.put("hasPassword", r.getPasswordHash() != null && !r.getPasswordHash().isEmpty());
            return map;
        }).collect(Collectors.toList());

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Map<String, Object>> resultPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        resultPage.setRecords(records);
        resultPage.setPages(p.getPages());
        return Result.ok(resultPage);
    }

    @PostMapping
    public Result<DiscussionRoom> create(@AuthenticationPrincipal SecurityUser user, @RequestBody @Valid RoomCreateDTO dto) {
        return Result.ok(roomService.createRoom(user.getUser().getId(), dto));
    }

    @PostMapping("/{id}/join")
    public Result<Void> join(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id, @RequestBody @Valid RoomJoinDTO dto) {
        roomService.joinRoom(user.getUser().getId(), id, dto.getPassword());
        return Result.ok();
    }

    @PostMapping("/{id}/close")
    public Result<Void> close(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {
        roomService.closeRoom(user.getUser().getId(), id);
        return Result.ok();
    }

    @GetMapping("/{id}/messages")
    public Result<List<ChatMessage>> messages(
            @PathVariable Long id,
            @RequestParam(required = false) Long lastSeq,
            @RequestParam(defaultValue = "50") Integer limit) {
        return Result.ok(roomService.messageHistory(id, lastSeq, limit));
    }

    @GetMapping("/{id}/whiteboard")
    public Result<List<WhiteboardOperation>> whiteboard(
            @PathVariable Long id,
            @RequestParam(required = false) Long lastSeq,
            @RequestParam(defaultValue = "200") Integer limit) {
        return Result.ok(roomService.whiteboardHistory(id, lastSeq, limit));
    }
}
