package com.anzs.module.notification.controller;

import com.anzs.common.Result;
import com.anzs.common.dto.BatchIdsDTO;
import com.anzs.config.security.SecurityUser;
import com.anzs.module.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Result<Map<String, Object>> list(
            @AuthenticationPrincipal SecurityUser user,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer isRead,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(notificationService.list(user.getUser().getId(), type, isRead, page, size));
    }

    @PutMapping("/read")
    public Result<Void> read(@AuthenticationPrincipal SecurityUser user, @RequestBody @Valid BatchIdsDTO dto) {
        notificationService.read(user.getUser().getId(), dto.getIds());
        return Result.ok();
    }

    @PutMapping("/read-all")
    public Result<Void> readAll(@AuthenticationPrincipal SecurityUser user) {
        notificationService.readAll(user.getUser().getId());
        return Result.ok();
    }
}
