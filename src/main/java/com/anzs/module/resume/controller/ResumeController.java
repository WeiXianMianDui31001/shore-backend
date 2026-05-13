package com.anzs.module.resume.controller;

import com.anzs.common.Result;
import com.anzs.config.security.SecurityUser;
import com.anzs.module.resume.dto.request.ResumePreviewHtmlDTO;
import com.anzs.module.resume.dto.request.ResumeSaveDTO;
import com.anzs.module.resume.dto.response.*;
import com.anzs.module.resume.service.PdfGenerationService;
import com.anzs.module.resume.service.ResumeExportService;
import com.anzs.module.resume.service.ResumeService;
import com.anzs.module.resume.service.ResumeImageUploadService;
import com.anzs.module.resume.service.ResumeTemplateService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeTemplateService resumeTemplateService;
    private final ResumeExportService resumeExportService;
    private final PdfGenerationService pdfGenerationService;
    private final ResumeImageUploadService resumeImageUploadService;

    // === Templates ===

    @GetMapping("/templates")
    public Result<List<ResumeTemplateVO>> templates(@RequestParam(required = false) Integer type) {
        return Result.ok(resumeTemplateService.listActiveTemplates(type));
    }

    @GetMapping("/templates/{id}")
    public Result<ResumeTemplateVO> templateDetail(@PathVariable Long id) {
        return Result.ok(resumeTemplateService.getTemplateDetail(id));
    }

    /** 实时 HTML 预览（与 PDF 使用同一套 Thymeleaf 模板与 enrich 逻辑） */
    @PostMapping("/preview-html")
    public Result<String> previewHtml(@Valid @RequestBody ResumePreviewHtmlDTO dto) {
        return Result.ok(pdfGenerationService.previewHtml(dto.getTemplateId(), dto.getContentJson()));
    }

    /** 证件照等：本地上传后得到 URL 写入「证件照」字段（需登录） */
    @PostMapping("/upload-photo")
    public Result<Map<String, String>> uploadPhoto(@AuthenticationPrincipal SecurityUser user,
                                                   @RequestParam("file") MultipartFile file) {
        return Result.ok(resumeImageUploadService.uploadResumePhoto(user.getUser().getId(), file));
    }

    // === Resume CRUD ===

    @PostMapping
    public Result<Map<String, Object>> create(@AuthenticationPrincipal SecurityUser user,
                                               @Valid @RequestBody ResumeSaveDTO dto) {
        return Result.ok(resumeService.createResume(user.getUser().getId(), dto));
    }

    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@AuthenticationPrincipal SecurityUser user,
                                               @PathVariable Long id,
                                               @Valid @RequestBody ResumeSaveDTO dto) {
        return Result.ok(resumeService.updateResume(user.getUser().getId(), id, dto));
    }

    @GetMapping("/my")
    public Result<IPage<ResumeVO>> my(@AuthenticationPrincipal SecurityUser user,
                                       @RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(resumeService.getMyResumes(user.getUser().getId(), page, size));
    }

    @GetMapping("/{id}")
    public Result<ResumeDetailVO> detail(@AuthenticationPrincipal SecurityUser user,
                                          @PathVariable Long id) {
        return Result.ok(resumeService.getResumeDetail(user.getUser().getId(), id));
    }

    @PostMapping("/{id}/duplicate")
    public Result<Map<String, Object>> duplicate(@AuthenticationPrincipal SecurityUser user,
                                                  @PathVariable Long id) {
        return Result.ok(resumeService.duplicateResume(user.getUser().getId(), id));
    }

    // === Version History ===

    @GetMapping("/{id}/versions")
    public Result<List<ResumeVersionVO>> versions(@AuthenticationPrincipal SecurityUser user,
                                                   @PathVariable Long id) {
        return Result.ok(resumeService.getVersions(user.getUser().getId(), id));
    }

    @GetMapping("/{id}/versions/{versionId}")
    public Result<ResumeVersionDetailVO> versionDetail(@AuthenticationPrincipal SecurityUser user,
                                                        @PathVariable Long id,
                                                        @PathVariable Long versionId) {
        return Result.ok(resumeService.getVersionDetail(user.getUser().getId(), id, versionId));
    }

    @PutMapping("/{id}/versions/{versionId}/restore")
    public Result<Map<String, Object>> restoreVersion(@AuthenticationPrincipal SecurityUser user,
                                                       @PathVariable Long id,
                                                       @PathVariable Long versionId) {
        return Result.ok(resumeService.restoreVersion(user.getUser().getId(), id, versionId));
    }

    // === Export ===

    @PostMapping("/{id}/export")
    public Result<ExportResultVO> export(@AuthenticationPrincipal SecurityUser user,
                                          @PathVariable Long id) {
        return Result.ok(resumeExportService.startExport(user.getUser().getId(), id));
    }

    @GetMapping("/{id}/export/{exportId}/status")
    public Result<ExportResultVO> exportStatus(@PathVariable Long exportId) {
        return Result.ok(resumeExportService.getExportStatus(exportId));
    }
}
