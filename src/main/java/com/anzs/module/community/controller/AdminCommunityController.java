package com.anzs.module.community.controller;

import com.anzs.common.Result;
import com.anzs.module.community.service.CommunityService;
import com.anzs.module.community.vo.PostVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/community")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommunityController {

    private final CommunityService communityService;

    @GetMapping("/posts")
    public Result<IPage<PostVO>> listPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(communityService.adminList(keyword, status, page, size));
    }

    @PutMapping("/posts/{id}/status")
    public Result<Void> updatePostStatus(@PathVariable Long id, @RequestParam Integer status) {
        communityService.updatePostStatus(id, status);
        return Result.ok();
    }

    @PutMapping("/posts/{id}/pin")
    public Result<Void> pinPost(@PathVariable Long id, @RequestParam Boolean pinned) {
        communityService.pinPost(id, pinned);
        return Result.ok();
    }

    @PutMapping("/comments/{id}/status")
    public Result<Void> updateCommentStatus(@PathVariable Long id, @RequestParam Integer status) {
        communityService.updateCommentStatus(id, status);
        return Result.ok();
    }
}
