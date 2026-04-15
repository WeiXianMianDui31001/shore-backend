package com.anzs.module.info.controller;

import com.anzs.common.Result;
import com.anzs.module.info.entity.InfoEntry;
import com.anzs.module.info.service.InfoService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/info")
@RequiredArgsConstructor
public class InfoController {

    private final InfoService infoService;

    @GetMapping
    public Result<IPage<InfoEntry>> list(
            @RequestParam(required = false) Integer scene,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(infoService.list(scene, category, keyword, page, size));
    }

    @GetMapping("/{id}")
    public Result<InfoEntry> detail(@PathVariable Long id) {
        return Result.ok(infoService.detail(id));
    }
}
