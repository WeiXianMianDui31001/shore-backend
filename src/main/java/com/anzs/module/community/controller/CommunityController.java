package com.anzs.module.community.controller;

import com.anzs.common.Result;
import com.anzs.config.security.SecurityUser;
import com.anzs.module.community.dto.CommentDTO;
import com.anzs.module.community.dto.PostCreateDTO;
import com.anzs.module.community.entity.Comment;
import com.anzs.module.community.entity.Post;
import com.anzs.module.community.service.CommunityService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @GetMapping
    public Result<IPage<Post>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(communityService.list(keyword, page, size));
    }

    @GetMapping("/{id}")
    public Result<Post> detail(@PathVariable Long id) {
        return Result.ok(communityService.detail(id));
    }

    @PostMapping
    public Result<Void> create(@AuthenticationPrincipal SecurityUser user,
                               @RequestBody @Valid PostCreateDTO dto) {
        communityService.createPost(user.getUser().getId(), dto.getTitle(), dto.getContent(), dto.getTags());
        return Result.ok();
    }

    @GetMapping("/{id}/comments")
    public Result<List<Comment>> comments(@PathVariable Long id) {
        return Result.ok(communityService.comments(id));
    }

    @PostMapping("/{id}/comments")
    public Result<Void> comment(@AuthenticationPrincipal SecurityUser user,
                                @PathVariable Long id,
                                @RequestBody @Valid CommentDTO dto) {
        communityService.addComment(user.getUser().getId(), id, dto.getParentId(), dto.getContent());
        return Result.ok();
    }

    @PutMapping("/{id}/resolve")
    public Result<Void> resolve(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {
        communityService.resolvePost(user.getUser().getId(), id);
        return Result.ok();
    }
}
