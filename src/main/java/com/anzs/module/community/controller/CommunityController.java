package com.anzs.module.community.controller;

import com.anzs.common.Result;
import com.anzs.config.security.SecurityUser;
import com.anzs.module.community.dto.CommentDTO;
import com.anzs.module.community.dto.PostCreateDTO;
import com.anzs.module.community.service.CommunityService;
import com.anzs.module.community.vo.CommentVO;
import com.anzs.module.community.vo.PostVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @GetMapping
    public Result<IPage<PostVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @AuthenticationPrincipal SecurityUser user) {
        Long userId = user != null ? user.getUser().getId() : null;
        return Result.ok(communityService.list(keyword, page, size, userId));
    }

    @GetMapping("/{id}")
    public Result<PostVO> detail(@PathVariable Long id,
                                 @AuthenticationPrincipal SecurityUser user) {
        Long userId = user != null ? user.getUser().getId() : null;
        return Result.ok(communityService.detail(id, userId));
    }

    @PostMapping
    public Result<Void> create(@AuthenticationPrincipal SecurityUser user,
                               @RequestBody @Valid PostCreateDTO dto) {
        communityService.createPost(user.getUser().getId(), dto);
        return Result.ok();
    }

    @PostMapping("/{id}/like")
    public Result<Void> like(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {
        communityService.likePost(user.getUser().getId(), id);
        return Result.ok();
    }

    @DeleteMapping("/{id}/like")
    public Result<Void> unlike(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {
        communityService.unlikePost(user.getUser().getId(), id);
        return Result.ok();
    }

    @PostMapping("/{id}/collect")
    public Result<Void> collect(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {
        communityService.collectPost(user.getUser().getId(), id);
        return Result.ok();
    }

    @DeleteMapping("/{id}/collect")
    public Result<Void> uncollect(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {
        communityService.uncollectPost(user.getUser().getId(), id);
        return Result.ok();
    }

    @GetMapping("/my-collects")
    public Result<IPage<PostVO>> myCollects(
            @AuthenticationPrincipal SecurityUser user,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(communityService.myCollects(user.getUser().getId(), page, size));
    }

    @GetMapping("/recommend")
    public Result<IPage<PostVO>> recommend(
            @AuthenticationPrincipal SecurityUser user,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Long userId = user != null ? user.getUser().getId() : null;
        Integer userRole = user != null ? user.getUser().getRole() : null;
        return Result.ok(communityService.recommendPosts(userId, userRole, page, size));
    }

    @PostMapping("/images/upload")
    public Result<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.ok(communityService.uploadImage(file));
    }

    @GetMapping("/{id}/comments")
    public Result<List<CommentVO>> comments(@PathVariable Long id) {
        return Result.ok(communityService.comments(id));
    }

    @PostMapping("/{id}/comments")
    public Result<Void> comment(@AuthenticationPrincipal SecurityUser user,
                                @PathVariable Long id,
                                @RequestBody @Valid CommentDTO dto) {
        String imagesJson = dto.getImages() != null && !dto.getImages().isEmpty()
                ? new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(dto.getImages()).toString()
                : null;
        communityService.addComment(user.getUser().getId(), id, dto.getParentId(), dto.getContent(), imagesJson);
        return Result.ok();
    }

    @PutMapping("/{id}/resolve")
    public Result<Void> resolve(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {
        communityService.resolvePost(user.getUser().getId(), id);
        return Result.ok();
    }

    @PostMapping("/{id}/endorse")
    public Result<Void> endorse(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {
        communityService.endorsePost(user.getUser().getId(), id);
        return Result.ok();
    }

    @DeleteMapping("/{id}/endorse")
    public Result<Void> unendorse(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {
        communityService.unendorsePost(user.getUser().getId(), id);
        return Result.ok();
    }
}
