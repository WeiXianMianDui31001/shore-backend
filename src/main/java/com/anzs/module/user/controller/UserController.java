package com.anzs.module.user.controller;

import com.anzs.common.Result;
import com.anzs.config.security.SecurityUser;
import com.anzs.module.user.dto.UserProfileDTO;
import com.anzs.module.user.entity.PointsTransaction;
import com.anzs.module.user.service.UserService;
import jakarta.validation.Valid;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public Result<Map<String, Object>> me(@AuthenticationPrincipal SecurityUser user) {
        return Result.ok(userService.getUserInfo(user.getUser().getId()));
    }

    @PutMapping("/me")
    public Result<Void> updateMe(@AuthenticationPrincipal SecurityUser user, @RequestBody @Valid UserProfileDTO dto) {
        userService.updateProfile(user.getUser().getId(), dto.getNickname(), dto.getAvatarUrl());
        return Result.ok();
    }

    @GetMapping("/points/summary")
    public Result<Map<String, Object>> pointsSummary(@AuthenticationPrincipal SecurityUser user) {
        return Result.ok(userService.getPointsSummary(user.getUser().getId()));
    }

    @GetMapping("/points/transactions")
    public Result<IPage<PointsTransaction>> transactions(
            @AuthenticationPrincipal SecurityUser user,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(userService.getPointsTransactions(user.getUser().getId(), page, size));
    }
}
