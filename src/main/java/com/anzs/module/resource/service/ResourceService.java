package com.anzs.module.resource.service;

import com.anzs.common.exception.BizException;
import com.anzs.common.util.JwtUtil;
import com.anzs.config.security.SecurityUser;
import com.anzs.infrastructure.cache.RedisCache;
import com.anzs.infrastructure.storage.AliOssService;
import com.anzs.module.resource.dto.ResourceSubmitDTO;
import com.anzs.module.resource.entity.DownloadRecord;
import com.anzs.module.resource.entity.Resource;
import com.anzs.module.resource.entity.ResourceAudit;
import com.anzs.module.resource.mapper.DownloadRecordMapper;
import com.anzs.module.resource.mapper.ResourceAuditMapper;
import com.anzs.module.resource.mapper.ResourceMapper;
import com.anzs.module.user.entity.PointsRule;
import com.anzs.module.user.entity.PointsTransaction;
import com.anzs.module.user.entity.SysUser;
import com.anzs.module.user.mapper.PointsRuleMapper;
import com.anzs.module.user.mapper.PointsTransactionMapper;
import com.anzs.module.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceMapper resourceMapper;
    private final ResourceAuditMapper resourceAuditMapper;
    private final DownloadRecordMapper downloadRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final PointsTransactionMapper pointsTransactionMapper;
    private final PointsRuleMapper pointsRuleMapper;
    private final RedisCache redisCache;
    private final AliOssService aliOssService;

    public IPage<Resource> list(String keyword, String category, Integer page, Integer size) {
        Page<Resource> p = new Page<>(page, size);
        LambdaQueryWrapper<Resource> qw = new LambdaQueryWrapper<>();
        qw.eq(Resource::getStatus, 1);
        if (category != null && !category.isEmpty()) {
            qw.eq(Resource::getCategory, category);
        }
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like(Resource::getTitle, keyword).or().like(Resource::getDescription, keyword));
        }
        qw.orderByDesc(Resource::getCreatedAt);
        return resourceMapper.selectPage(p, qw);
    }

    public IPage<Resource> myUploads(Long userId, Integer status, Integer page, Integer size) {
        Page<Resource> p = new Page<>(page, size);
        LambdaQueryWrapper<Resource> qw = new LambdaQueryWrapper<>();
        qw.eq(Resource::getUploaderId, userId);
        if (status != null) qw.eq(Resource::getStatus, status);
        qw.orderByDesc(Resource::getCreatedAt);
        return resourceMapper.selectPage(p, qw);
    }

    public Map<String, Object> detail(Long id, Long currentUserId) {
        Resource r = resourceMapper.selectById(id);
        if (r == null) throw new BizException("资源不存在");
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("title", r.getTitle());
        map.put("description", r.getDescription());
        map.put("category", r.getCategory());
        map.put("tags", r.getTags());
        map.put("fileSize", r.getFileSize());
        map.put("fileType", r.getFileType());
        map.put("pointsCost", r.getPointsCost());
        map.put("downloadCount", r.getDownloadCount());
        map.put("status", r.getStatus());
        map.put("uploaderId", r.getUploaderId());
        boolean showUrl = currentUserId != null && (currentUserId.equals(r.getUploaderId())
                || hasDownloaded(currentUserId, r.getId()));
        map.put("fileUrl", showUrl ? r.getFileUrl() : null);
        return map;
    }

    private boolean hasDownloaded(Long userId, Long resourceId) {
        String key = "download:" + userId + ":" + resourceId;
        return Boolean.TRUE.equals(redisCache.hasKey(key))
                || downloadRecordMapper.selectCount(new LambdaQueryWrapper<DownloadRecord>()
                .eq(DownloadRecord::getUserId, userId)
                .eq(DownloadRecord::getResourceId, resourceId)) > 0;
    }

    public Map<String, Object> prepareUpload(String fileName, Long fileSize, String mimeType) {
        // 简化：直接返回本地临时上传地址，实际生产应对接 OSS 预签名
        String uploadId = java.util.UUID.randomUUID().toString();
        Map<String, Object> map = new HashMap<>();
        map.put("uploadId", uploadId);
        map.put("preSignedUrl", aliOssService.generateUploadUrl(uploadId + "-" + fileName));
        map.put("expireSeconds", 300);
        return map;
    }

    @Transactional
    public void submit(Long userId, ResourceSubmitDTO dto) {
        Resource r = new Resource();
        r.setUploaderId(userId);
        r.setTitle(dto.getTitle());
        r.setCategory(dto.getCategory());
        r.setTags(dto.getTags());
        r.setDescription(dto.getDescription());
        r.setFileUrl(dto.getUploadId()); // 简化：uploadId 即文件地址
        r.setStatus(0);
        r.setPointsCost(5);
        r.setDownloadCount(0);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        resourceMapper.insert(r);
    }

    @Transactional
    public Map<String, Object> download(Long userId, Long resourceId) {
        Resource r = resourceMapper.selectById(resourceId);
        if (r == null || r.getStatus() != 1) throw new BizException("资源不存在或未上架");
        if (userId.equals(r.getUploaderId())) {
            Map<String, Object> map = new HashMap<>();
            map.put("fileUrl", r.getFileUrl());
            map.put("costPoints", 0);
            return map;
        }
        String dupKey = "download:" + userId + ":" + resourceId;
        boolean already = redisCache.hasKey(dupKey);
        if (!already) {
            already = downloadRecordMapper.selectCount(new LambdaQueryWrapper<DownloadRecord>()
                    .eq(DownloadRecord::getUserId, userId)
                    .eq(DownloadRecord::getResourceId, resourceId)) > 0;
        }
        if (already) {
            redisCache.set(dupKey, "1", 24, TimeUnit.HOURS);
            Map<String, Object> map = new HashMap<>();
            map.put("fileUrl", r.getFileUrl());
            map.put("costPoints", 0);
            return map;
        }

        SysUser downloader = sysUserMapper.selectById(userId);
        if (downloader.getPointsBalance() < r.getPointsCost()) {
            throw new BizException("积分不足");
        }

        PointsRule rule = pointsRuleMapper.selectCurrentActive();
        int reward = 0;
        if (rule != null && rule.getShareRatio() != null) {
            reward = (int) (r.getPointsCost() * rule.getShareRatio().doubleValue());
        }

        // 扣减下载者积分
        int rows = sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .ge(SysUser::getPointsBalance, r.getPointsCost())
                .setSql("points_balance = points_balance - " + r.getPointsCost())
                .set(SysUser::getUpdatedAt, LocalDateTime.now()));
        if (rows == 0) throw new BizException("积分不足或并发冲突，请重试");

        // 增加上传者积分
        if (reward > 0) {
            sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                    .eq(SysUser::getId, r.getUploaderId())
                    .setSql("points_balance = points_balance + " + reward)
                    .set(SysUser::getUpdatedAt, LocalDateTime.now()));
        }

        // 流水
        PointsTransaction txOut = new PointsTransaction();
        txOut.setUserId(userId);
        txOut.setType(2);
        txOut.setAmount(-r.getPointsCost());
        txOut.setBalanceAfter(downloader.getPointsBalance() - r.getPointsCost());
        txOut.setSourceType("DOWNLOAD");
        txOut.setBizId(resourceId);
        txOut.setCreatedAt(LocalDateTime.now());
        pointsTransactionMapper.insert(txOut);

        if (reward > 0) {
            SysUser uploader = sysUserMapper.selectById(r.getUploaderId());
            PointsTransaction txIn = new PointsTransaction();
            txIn.setUserId(r.getUploaderId());
            txIn.setType(1);
            txIn.setAmount(reward);
            txIn.setBalanceAfter(uploader.getPointsBalance() + reward);
            txIn.setSourceType("DOWNLOAD_REWARD");
            txIn.setBizId(resourceId);
            txIn.setCreatedAt(LocalDateTime.now());
            pointsTransactionMapper.insert(txIn);
        }

        // 下载记录
        DownloadRecord dr = new DownloadRecord();
        dr.setUserId(userId);
        dr.setResourceId(resourceId);
        dr.setCostPoints(r.getPointsCost());
        dr.setUploaderReward(reward);
        dr.setCreatedAt(LocalDateTime.now());
        downloadRecordMapper.insert(dr);

        // 更新下载次数
        resourceMapper.update(null, new LambdaUpdateWrapper<Resource>()
                .eq(Resource::getId, resourceId)
                .setSql("download_count = download_count + 1"));

        redisCache.set(dupKey, "1", 24, TimeUnit.HOURS);

        Map<String, Object> map = new HashMap<>();
        map.put("fileUrl", r.getFileUrl());
        map.put("costPoints", r.getPointsCost());
        return map;
    }
}
