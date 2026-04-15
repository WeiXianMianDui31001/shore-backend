package com.anzs.module.user.controller;

import com.anzs.common.Result;
import com.anzs.config.security.SecurityUser;
import com.anzs.module.user.dto.FavoriteDTO;
import com.anzs.module.user.entity.UserFavorite;
import com.anzs.module.user.service.FavoriteService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping
    public Result<Void> add(@AuthenticationPrincipal SecurityUser user, @RequestBody @Valid FavoriteDTO dto) {
        favoriteService.addFavorite(user.getUser().getId(), dto.getTargetType(), dto.getTargetId());
        return Result.ok();
    }

    @DeleteMapping
    public Result<Void> remove(@AuthenticationPrincipal SecurityUser user, @RequestBody @Valid FavoriteDTO dto) {
        favoriteService.removeFavorite(user.getUser().getId(), dto.getTargetType(), dto.getTargetId());
        return Result.ok();
    }

    @GetMapping
    public Result<IPage<UserFavorite>> list(
            @AuthenticationPrincipal SecurityUser user,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(favoriteService.listFavorites(user.getUser().getId(), targetType, page, size));
    }
}
