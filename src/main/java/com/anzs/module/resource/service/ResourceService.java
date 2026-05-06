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
import com.anzs.module.user.entity.UserFavorite;
import com.anzs.module.user.mapper.PointsRuleMapper;
import com.anzs.module.user.mapper.PointsTransactionMapper;
import com.anzs.module.user.mapper.SysUserMapper;
import com.anzs.module.user.mapper.UserFavoriteMapper;
import com.anzs.module.user.service.PointsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceMapper resourceMapper;
    private final ResourceAuditMapper resourceAuditMapper;
    private final DownloadRecordMapper downloadRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final PointsTransactionMapper pointsTransactionMapper;
    private final PointsRuleMapper pointsRuleMapper;
    private final PointsService pointsService;
    private final RedisCache redisCache;
    private final AliOssService aliOssService;
    private final UserFavoriteMapper userFavoriteMapper;

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
        map.put("fileSize", r.getFileSize());
        map.put("fileType", r.getFileType());
        map.put("pointsCost", r.getPointsCost());
        map.put("downloadCount", r.getDownloadCount());
        map.put("status", r.getStatus());
        map.put("uploaderId", r.getUploaderId());
        boolean canDownload = currentUserId != null && (currentUserId.equals(r.getUploaderId())
                || hasDownloaded(currentUserId, r.getId()));
        boolean collected = currentUserId != null && userFavoriteMapper.selectCount(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, currentUserId)
                        .eq(UserFavorite::getTargetType, "RESOURCE")
                        .eq(UserFavorite::getTargetId, id)) > 0;
        map.put("canDownload", canDownload);
        map.put("collected", collected);
        map.put("fileUrl", null); // 不直接暴露原始 OSS URL
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
        String uploadId = java.util.UUID.randomUUID().toString();
        String objectKey = uploadId + "-" + fileName;

        Map<String, Object> meta = new HashMap<>();
        meta.put("objectKey", objectKey);
        meta.put("fileSize", fileSize);
        meta.put("fileType", mimeType);
        redisCache.setObject("upload:meta:" + uploadId, meta, 10, TimeUnit.MINUTES);

        Map<String, Object> map = new HashMap<>();
        map.put("uploadId", uploadId);
        map.put("preSignedUrl", aliOssService.generateUploadUrl(objectKey, mimeType));
        map.put("expireSeconds", 300);
        return map;
    }

    @Transactional
    public void submit(Long userId, ResourceSubmitDTO dto) {
        String uploadId = dto.getUploadId();
        @SuppressWarnings("unchecked")
        Map<String, Object> meta = redisCache.getObject("upload:meta:" + uploadId, Map.class);
        if (meta == null) {
            throw new BizException("上传会话已过期，请重新上传");
        }
        String objectKey = (String) meta.get("objectKey");
        Object fileSizeObj = meta.get("fileSize");
        Long fileSize = fileSizeObj instanceof Number ? ((Number) fileSizeObj).longValue() : null;
        String fileType = (String) meta.get("fileType");

        Resource r = new Resource();
        r.setUploaderId(userId);
        r.setTitle(dto.getTitle());
        r.setCategory(dto.getCategory());
        r.setDescription(dto.getDescription());
        r.setObjectKey(objectKey); // 存 objectKey，供下载时生成预签名URL
        r.setFileSize(fileSize);
        r.setFileType(fileType);
        r.setStatus(0);
        PointsRule rule = pointsRuleMapper.selectCurrentActive();
        r.setPointsCost(rule != null && rule.getDownloadCost() != null ? rule.getDownloadCost() : 5);
        r.setDownloadCount(0);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        resourceMapper.insert(r);

        redisCache.delete("upload:meta:" + uploadId);
    }

    @Transactional
    public Map<String, Object> download(Long userId, Long resourceId) {
        Resource r = resourceMapper.selectById(resourceId);
        if (r == null || r.getStatus() != 1) throw new BizException("资源不存在或未上架");

        // 上传者本人下载：不计次数，不扣积分
        if (userId.equals(r.getUploaderId())) {
            log.info("资源[{}]被上传者[{}]本人下载，跳过计数", resourceId, userId);
            Map<String, Object> map = new HashMap<>();
            map.put("fileUrl", aliOssService.generateDownloadUrl(r.getObjectKey()));
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
            log.info("用户[{}]重复下载资源[{}]，直接返回链接", userId, resourceId);
            redisCache.set(dupKey, "1", 24, TimeUnit.HOURS);
            Map<String, Object> map = new HashMap<>();
            map.put("fileUrl", aliOssService.generateDownloadUrl(r.getObjectKey()));
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

        // 扣减下载者积分（走统一积分服务）
        boolean ok = pointsService.deductPoints(userId, r.getPointsCost(), "DOWNLOAD_COST", resourceId, "下载资源");
        if (!ok) throw new BizException("积分不足或并发冲突，请重试");

        // 上传者分成（走统一积分服务）
        if (reward > 0) {
            pointsService.addPoints(r.getUploaderId(), reward, "DOWNLOAD_SHARE", resourceId, "资源被下载奖励");
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
        int rows = resourceMapper.update(null, new LambdaUpdateWrapper<Resource>()
                .eq(Resource::getId, resourceId)
                .setSql("download_count = download_count + 1"));
        log.info("用户[{}]首次下载资源[{}]，扣除积分[{}]，上传者分成[{}]，更新下载次数影响行数=[{}]",
                userId, resourceId, r.getPointsCost(), reward, rows);

        redisCache.set(dupKey, "1", 24, TimeUnit.HOURS);

        Map<String, Object> map = new HashMap<>();
        map.put("fileUrl", aliOssService.generateDownloadUrl(r.getObjectKey()));
        map.put("costPoints", r.getPointsCost());
        return map;
    }

    // ========== Favorite / Collect ==========

    @Transactional
    public void collectResource(Long userId, Long resourceId) {
        Resource r = resourceMapper.selectById(resourceId);
        if (r == null) throw new BizException("资源不存在");
        long count = userFavoriteMapper.selectCount(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getTargetType, "RESOURCE")
                .eq(UserFavorite::getTargetId, resourceId));
        if (count > 0) return;
        UserFavorite f = new UserFavorite();
        f.setUserId(userId);
        f.setTargetType("RESOURCE");
        f.setTargetId(resourceId);
        f.setCreatedAt(LocalDateTime.now());
        userFavoriteMapper.insert(f);
    }

    @Transactional
    public void uncollectResource(Long userId, Long resourceId) {
        long count = userFavoriteMapper.selectCount(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getTargetType, "RESOURCE")
                .eq(UserFavorite::getTargetId, resourceId));
        if (count == 0) return;
        userFavoriteMapper.delete(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getTargetType, "RESOURCE")
                .eq(UserFavorite::getTargetId, resourceId));
    }

    public IPage<Resource> myFavorites(Long userId, Integer page, Integer size) {
        Page<UserFavorite> p = new Page<>(page, size);
        LambdaQueryWrapper<UserFavorite> qw = new LambdaQueryWrapper<>();
        qw.eq(UserFavorite::getUserId, userId)
          .eq(UserFavorite::getTargetType, "RESOURCE")
          .orderByDesc(UserFavorite::getCreatedAt);
        IPage<UserFavorite> favPage = userFavoriteMapper.selectPage(p, qw);

        List<Long> resourceIds = favPage.getRecords().stream()
                .map(UserFavorite::getTargetId)
                .collect(java.util.stream.Collectors.toList());

        List<Resource> resources = resourceIds.isEmpty() ? java.util.Collections.emptyList() :
                resourceMapper.selectList(new LambdaQueryWrapper<Resource>()
                        .in(Resource::getId, resourceIds));

        Map<Long, Resource> resourceMap = resources.stream()
                .collect(java.util.stream.Collectors.toMap(Resource::getId, r -> r));

        List<Resource> ordered = favPage.getRecords().stream()
                .map(f -> resourceMap.get(f.getTargetId()))
                .filter(r -> r != null)
                .collect(java.util.stream.Collectors.toList());

        Page<Resource> result = new Page<>(page, size);
        result.setRecords(ordered);
        result.setTotal(favPage.getTotal());
        return result;
    }
}
