package com.anzs.module.resume.controller;

import com.anzs.common.Result;
import com.anzs.config.security.SecurityUser;
import com.anzs.module.resume.entity.Resume;
import com.anzs.module.resume.entity.ResumeTemplate;
import com.anzs.module.resume.service.ResumeService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping("/templates")
    public Result<List<ResumeTemplate>> templates(@RequestParam(required = false) Integer type) {
        return Result.ok(resumeService.templateList(type));
    }

    @PostMapping
    public Result<Map<String, Object>> save(@AuthenticationPrincipal SecurityUser user, @RequestBody Resume resume) {
        return Result.ok(resumeService.saveResume(user.getUser().getId(), resume));
    }

    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id, @RequestBody Resume resume) {
        resume.setId(id);
        return Result.ok(resumeService.saveResume(user.getUser().getId(), resume));
    }

    @GetMapping("/my")
    public Result<IPage<Resume>> my(
            @AuthenticationPrincipal SecurityUser user,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(resumeService.myResumes(user.getUser().getId(), page, size));
    }

    @GetMapping("/{id}")
    public Result<Resume> detail(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {
        return Result.ok(resumeService.getResume(user.getUser().getId(), id));
    }

    @PostMapping("/{id}/export")
    public Result<Map<String, Object>> export(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {
        return Result.ok(resumeService.exportResume(user.getUser().getId(), id));
    }

    @GetMapping("/{id}/export/{exportId}/status")
    public Result<Map<String, Object>> exportStatus(@PathVariable Long exportId) {
        var export = resumeService.getExportStatus(exportId);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("exportId", export.getId());
        result.put("status", export.getPdfUrl() == null ? "PROCESSING" : "SUCCESS");
        result.put("pdfUrl", export.getPdfUrl());
        return Result.ok(result);
    }
}
