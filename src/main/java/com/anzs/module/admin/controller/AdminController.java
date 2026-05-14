package com.anzs.module.admin.controller;

import com.anzs.common.Result;
import com.anzs.common.dto.BatchIdsDTO;
import com.anzs.config.security.SecurityUser;
import com.anzs.module.admin.dto.AuditDTO;
import com.anzs.module.admin.dto.CommunityActionDTO;
import com.anzs.module.admin.dto.PointsRuleUpdateDTO;
import com.anzs.module.admin.dto.ResetPasswordDTO;
import com.anzs.module.admin.dto.UserStatusDTO;
import com.anzs.module.admin.service.AdminService;
import com.anzs.module.community.entity.Comment;
import com.anzs.module.community.entity.Post;
import com.anzs.module.info.entity.InfoEntry;
import com.anzs.module.resource.entity.Resource;
import com.anzs.module.user.entity.PointsRule;
import com.anzs.module.user.entity.SysUser;
import com.anzs.module.resume.dto.request.ResumeTemplateSaveDTO;
import com.anzs.module.resume.dto.response.ResumeTemplateAdminVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // === 资源审核 ===
    @GetMapping("/resources/audit")
    public Result<IPage<Resource>> auditList(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(adminService.auditList(status, page, size));
    }

    @GetMapping("/resources/{id}/preview")
    public Result<String> previewResource(@PathVariable Long id) {
        return Result.ok(adminService.previewResource(id));
    }

    @PostMapping("/resources/{id}/audit")
    public Result<Void> auditResource(@AuthenticationPrincipal SecurityUser user,
                                      @PathVariable Long id,
                                      @RequestBody @Valid AuditDTO dto) {
        adminService.auditResource(user.getUser().getId(), id, dto.getAction(), dto.getReason() == null ? "" : dto.getReason());
        return Result.ok();
    }

    // === 信息库 ===
    @GetMapping("/info")
    public Result<IPage<InfoEntry>> infoList(
            @RequestParam(required = false) Integer scene,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(adminService.infoList(scene, status, page, size));
    }

    @PostMapping("/info")
    public Result<Void> saveInfo(@AuthenticationPrincipal SecurityUser user, @RequestBody InfoEntry entry) {
        adminService.saveInfo(entry, user.getUser().getId());
        return Result.ok();
    }

    @PutMapping("/info/{id}")
    public Result<Void> updateInfo(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id, @RequestBody InfoEntry entry) {
        entry.setId(id);
        adminService.saveInfo(entry, user.getUser().getId());
        return Result.ok();
    }

    @PutMapping("/info/batch-offline")
    public Result<Void> batchOfflineInfo(@AuthenticationPrincipal SecurityUser user, @RequestBody @Valid BatchIdsDTO dto) {
        adminService.batchOfflineInfo(dto.getIds(), user.getUser().getId());
        return Result.ok();
    }

    // === 积分规则 ===
    @GetMapping("/points-rule/current")
    public Result<PointsRule> currentRule() {
        return Result.ok(adminService.currentRule());
    }

    @GetMapping("/points-rule/history")
    public Result<IPage<PointsRule>> ruleHistory(@RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(adminService.ruleHistory(page, size));
    }

    @PostMapping("/points-rule")
    public Result<Void> publishRule(@AuthenticationPrincipal SecurityUser user, @RequestBody PointsRule rule) {
        adminService.publishRule(user.getUser().getId(), rule);
        return Result.ok();
    }

    @PostMapping("/points-rule/{version}/rollback")
    public Result<Void> rollbackRule(@AuthenticationPrincipal SecurityUser user, @PathVariable Integer version) {
        adminService.rollbackRule(user.getUser().getId(), version);
        return Result.ok();
    }

    @PutMapping("/points-rule/current")
    public Result<Void> updateCurrentRule(@AuthenticationPrincipal SecurityUser user,
                                          @RequestBody @Valid PointsRuleUpdateDTO dto) {
        adminService.updatePointsRuleCurrent(user.getUser().getId(), dto);
        return Result.ok();
    }

    // === 账号管理 ===
    @GetMapping("/users")
    public Result<IPage<SysUser>> userList(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer role,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(adminService.userList(status, role, keyword, page, size));
    }

    @GetMapping("/users/{id}")
    public Result<SysUser> userDetail(@PathVariable Long id) {
        return Result.ok(adminService.userDetail(id));
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@AuthenticationPrincipal SecurityUser user,
                                         @PathVariable Long id,
                                         @RequestBody @Valid UserStatusDTO dto) {
        adminService.updateUserStatus(user.getUser().getId(), id, dto.getStatus(), dto.getReason() == null ? "" : dto.getReason());
        return Result.ok();
    }

    @PutMapping("/users/{id}/password")
    public Result<Void> resetUserPassword(@AuthenticationPrincipal SecurityUser user,
                                          @PathVariable Long id,
                                          @RequestBody @Valid ResetPasswordDTO dto) {
        adminService.resetUserPassword(user.getUser().getId(), id, dto.getNewPassword());
        return Result.ok();
    }

    // === 社区巡检 ===
    @PostMapping("/community/{targetType}/{targetId}/action")
    public Result<Void> communityAction(@AuthenticationPrincipal SecurityUser user,
                                        @PathVariable String targetType,
                                        @PathVariable Long targetId,
                                        @RequestBody @Valid CommunityActionDTO dto) {
        adminService.communityAction(user.getUser().getId(), targetType, targetId, dto.getAction(), dto.getReason());
        return Result.ok();
    }

    // === 帖子加精 ===
    @GetMapping("/community/posts/pending-excellent")
    public Result<IPage<com.anzs.module.community.vo.PostVO>> pendingExcellentPosts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(adminService.pendingExcellentPosts(page, size));
    }

    @PostMapping("/community/posts/{id}/excellent")
    public Result<Void> excellentPost(@AuthenticationPrincipal SecurityUser user,
                                      @PathVariable Long id) {
        adminService.excellentPost(user.getUser().getId(), id);
        return Result.ok();
    }

    // === 简历模板管理 ===
    @GetMapping("/resume-templates")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<ResumeTemplateAdminVO>> resumeTemplates(
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.ok(adminService.listResumeTemplates(type, status, page, size));
    }

    @PostMapping("/resume-templates")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> createResumeTemplate(@AuthenticationPrincipal SecurityUser user,
                                              @Valid @RequestBody ResumeTemplateSaveDTO dto) {
        adminService.createResumeTemplate(user.getUser().getId(), dto);
        return Result.ok();
    }

    @PutMapping("/resume-templates/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateResumeTemplate(@AuthenticationPrincipal SecurityUser user,
                                              @PathVariable Long id,
                                              @Valid @RequestBody ResumeTemplateSaveDTO dto) {
        adminService.updateResumeTemplate(user.getUser().getId(), id, dto);
        return Result.ok();
    }

    @DeleteMapping("/resume-templates/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteResumeTemplate(@AuthenticationPrincipal SecurityUser user,
                                              @PathVariable Long id) {
        adminService.deleteResumeTemplate(user.getUser().getId(), id);
        return Result.ok();
    }
}
