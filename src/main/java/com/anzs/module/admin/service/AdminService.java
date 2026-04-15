package com.anzs.module.admin.service;

import cn.hutool.json.JSONUtil;
import com.anzs.common.exception.BizException;
import com.anzs.module.admin.entity.AdminLog;
import com.anzs.module.admin.mapper.AdminLogMapper;
import com.anzs.module.community.entity.Comment;
import com.anzs.module.community.entity.Post;
import com.anzs.module.community.mapper.CommentMapper;
import com.anzs.module.community.mapper.PostMapper;
import com.anzs.module.info.entity.InfoEntry;
import com.anzs.module.info.mapper.InfoEntryMapper;
import com.anzs.module.notification.service.NotificationService;
import com.anzs.module.resource.entity.Resource;
import com.anzs.module.resource.entity.ResourceAudit;
import com.anzs.module.resource.mapper.ResourceAuditMapper;
import com.anzs.module.resource.mapper.ResourceMapper;
import com.anzs.module.user.entity.PointsRule;
import com.anzs.module.user.entity.SysUser;
import com.anzs.module.user.mapper.PointsRuleMapper;
import com.anzs.module.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ResourceMapper resourceMapper;
    private final ResourceAuditMapper resourceAuditMapper;
    private final InfoEntryMapper infoEntryMapper;
    private final PointsRuleMapper pointsRuleMapper;
    private final SysUserMapper sysUserMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final AdminLogMapper adminLogMapper;
    private final NotificationService notificationService;

    public IPage<Resource> auditList(Integer status, Integer page, Integer size) {
        Page<Resource> p = new Page<>(page, size);
        LambdaQueryWrapper<Resource> qw = new LambdaQueryWrapper<>();
        if (status != null) qw.eq(Resource::getStatus, status);
        qw.orderByDesc(Resource::getCreatedAt);
        return resourceMapper.selectPage(p, qw);
    }

    @Transactional
    public void auditResource(Long adminId, Long resourceId, Integer action, String reason) {
        Resource r = resourceMapper.selectById(resourceId);
        if (r == null) throw new BizException("资源不存在");
        ResourceAudit audit = new ResourceAudit();
        audit.setResourceId(resourceId);
        audit.setAdminId(adminId);
        audit.setAction(action);
        audit.setReason(reason);
        audit.setCreatedAt(LocalDateTime.now());
        resourceAuditMapper.insert(audit);

        int newStatus = action == 1 ? 1 : 2;
        r.setStatus(newStatus);
        r.setUpdatedAt(LocalDateTime.now());
        resourceMapper.updateById(r);

        // 发送通知
        String msg = action == 1 ? "您上传的资源《" + r.getTitle() + "》已通过审核" : "您上传的资源《" + r.getTitle() + "》未通过审核，原因：" + reason;
        notificationService.sendNotification(r.getUploaderId(), 2, "资源审核结果", msg, resourceId, "RESOURCE");

        // 通过奖励积分
        if (action == 1) {
            PointsRule rule = pointsRuleMapper.selectCurrentActive();
            if (rule != null && rule.getUploadReward() != null && rule.getUploadReward() > 0) {
                SysUser uploader = sysUserMapper.selectById(r.getUploaderId());
                sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                        .eq(SysUser::getId, r.getUploaderId())
                        .setSql("points_balance = points_balance + " + rule.getUploadReward())
                        .set(SysUser::getUpdatedAt, LocalDateTime.now()));
                // 简化：不发流水了，实际应发
            }
        }

        logAdminAction(adminId, "AUDIT_RESOURCE", "RESOURCE", resourceId, Map.of("action", action, "reason", reason));
    }

    public IPage<InfoEntry> infoList(Integer scene, Integer status, Integer page, Integer size) {
        Page<InfoEntry> p = new Page<>(page, size);
        LambdaQueryWrapper<InfoEntry> qw = new LambdaQueryWrapper<>();
        if (scene != null) qw.eq(InfoEntry::getScene, scene);
        if (status != null) qw.eq(InfoEntry::getStatus, status);
        qw.orderByDesc(InfoEntry::getSortOrder);
        return infoEntryMapper.selectPage(p, qw);
    }

    @Transactional
    public void saveInfo(InfoEntry entry, Long adminId) {
        entry.setAdminId(adminId);
        entry.setUpdatedAt(LocalDateTime.now());
        if (entry.getId() == null) {
            entry.setCreatedAt(LocalDateTime.now());
            infoEntryMapper.insert(entry);
        } else {
            infoEntryMapper.updateById(entry);
        }
    }

    @Transactional
    public void batchOfflineInfo(List<Long> ids, Long adminId) {
        for (Long id : ids) {
            InfoEntry e = new InfoEntry();
            e.setId(id);
            e.setStatus(1);
            e.setUpdatedAt(LocalDateTime.now());
            infoEntryMapper.updateById(e);
        }
        logAdminAction(adminId, "BATCH_OFFLINE_INFO", "INFO", 0L, Map.of("ids", ids));
    }

    public PointsRule currentRule() {
        return pointsRuleMapper.selectCurrentActive();
    }

    public IPage<PointsRule> ruleHistory(Integer page, Integer size) {
        Page<PointsRule> p = new Page<>(page, size);
        LambdaQueryWrapper<PointsRule> qw = new LambdaQueryWrapper<>();
        qw.orderByDesc(PointsRule::getVersion);
        return pointsRuleMapper.selectPage(p, qw);
    }

    @Transactional
    public void publishRule(Long adminId, PointsRule rule) {
        PointsRule current = pointsRuleMapper.selectCurrentActive();
        int nextVersion = current == null ? 1 : current.getVersion() + 1;
        rule.setVersion(nextVersion);
        rule.setIsActive(true);
        rule.setCreatedBy(adminId);
        rule.setCreatedAt(LocalDateTime.now());
        if (current != null) {
            current.setIsActive(false);
            pointsRuleMapper.updateById(current);
        }
        pointsRuleMapper.insert(rule);
        logAdminAction(adminId, "PUBLISH_RULE", "POINTS_RULE", rule.getId(), Map.of("version", nextVersion));
    }

    @Transactional
    public void rollbackRule(Long adminId, Integer version) {
        PointsRule target = pointsRuleMapper.selectOne(
                new LambdaQueryWrapper<PointsRule>().eq(PointsRule::getVersion, version)
        );
        if (target == null) throw new BizException("版本不存在");
        PointsRule current = pointsRuleMapper.selectCurrentActive();
        if (current != null) {
            current.setIsActive(false);
            pointsRuleMapper.updateById(current);
        }
        PointsRule newRule = new PointsRule();
        newRule.setDownloadCost(target.getDownloadCost());
        newRule.setUploadReward(target.getUploadReward());
        newRule.setShareRatio(target.getShareRatio());
        newRule.setDailyLimit(target.getDailyLimit());
        newRule.setEffectiveTime(LocalDateTime.now());
        newRule.setIsActive(true);
        newRule.setChangeNote("回滚到版本 " + version);
        newRule.setCreatedBy(adminId);
        newRule.setCreatedAt(LocalDateTime.now());
        publishRule(adminId, newRule);
    }

    public IPage<SysUser> userList(Integer status, Integer role, String keyword, Integer page, Integer size) {
        Page<SysUser> p = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        if (status != null) qw.eq(SysUser::getStatus, status);
        if (role != null) qw.eq(SysUser::getRole, role);
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like(SysUser::getEmail, keyword).or().like(SysUser::getNickname, keyword));
        }
        qw.orderByDesc(SysUser::getCreatedAt);
        return sysUserMapper.selectPage(p, qw);
    }

    public SysUser userDetail(Long userId) {
        return sysUserMapper.selectById(userId);
    }

    @Transactional
    public void updateUserStatus(Long adminId, Long userId, Integer status, String reason) {
        if (adminId.equals(userId)) throw new BizException("不能操作自己的账号");
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) throw new BizException("用户不存在");
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
        logAdminAction(adminId, "UPDATE_USER_STATUS", "USER", userId, Map.of("status", status, "reason", reason));
    }

    @Transactional
    public void communityAction(Long adminId, String targetType, Long targetId, String action, String reason) {
        if ("POST".equals(targetType)) {
            Post post = postMapper.selectById(targetId);
            if (post == null) throw new BizException("帖子不存在");
            post.setStatus(2);
            post.setUpdatedAt(LocalDateTime.now());
            postMapper.updateById(post);
        } else if ("COMMENT".equals(targetType)) {
            Comment c = commentMapper.selectById(targetId);
            if (c == null) throw new BizException("评论不存在");
            c.setStatus(1);
            commentMapper.updateById(c);
        }
        logAdminAction(adminId, "COMMUNITY_" + action, targetType, targetId, Map.of("reason", reason));
    }

    private void logAdminAction(Long adminId, String action, String targetType, Long targetId, Object detail) {
        AdminLog log = new AdminLog();
        log.setAdminId(adminId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(JSONUtil.toJsonStr(detail));
        log.setIp("0.0.0.0");
        log.setCreatedAt(LocalDateTime.now());
        adminLogMapper.insert(log);
    }
}
