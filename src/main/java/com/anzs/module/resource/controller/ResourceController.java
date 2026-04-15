package com.anzs.module.resource.controller;

import com.anzs.common.Result;
import com.anzs.config.security.SecurityUser;
import com.anzs.module.resource.dto.ResourceSubmitDTO;
import com.anzs.module.resource.dto.UploadPrepareDTO;
import com.anzs.module.resource.entity.Resource;
import com.anzs.module.resource.service.ResourceService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public Result<IPage<Resource>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(resourceService.list(keyword, category, page, size));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id,
                                             @AuthenticationPrincipal SecurityUser user) {
        Long userId = user == null ? null : user.getUser().getId();
        return Result.ok(resourceService.detail(id, userId));
    }

    @PostMapping("/upload/prepare")
    public Result<Map<String, Object>> prepareUpload(@RequestBody @Valid UploadPrepareDTO dto) {
        String mimeType = dto.getMimeType() == null ? "" : dto.getMimeType();
        return Result.ok(resourceService.prepareUpload(dto.getFileName(), dto.getFileSize(), mimeType));
    }

    @PostMapping
    public Result<Void> submit(@AuthenticationPrincipal SecurityUser user,
                               @RequestBody @Valid ResourceSubmitDTO dto) {
        resourceService.submit(user.getUser().getId(), dto);
        return Result.ok();
    }

    @PostMapping("/{id}/download")
    public Result<Map<String, Object>> download(@AuthenticationPrincipal SecurityUser user,
                                                @PathVariable Long id) {
        return Result.ok(resourceService.download(user.getUser().getId(), id));
    }

    @GetMapping("/my")
    public Result<IPage<Resource>> my(
            @AuthenticationPrincipal SecurityUser user,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(resourceService.myUploads(user.getUser().getId(), status, page, size));
    }
}
