package com.anzs.module.resume.service;

import com.anzs.common.exception.BizException;
import com.anzs.module.resume.dto.request.ResumeSaveDTO;
import com.anzs.module.resume.dto.response.*;
import com.anzs.module.resume.entity.Resume;
import com.anzs.module.resume.entity.ResumeTemplate;
import com.anzs.module.resume.entity.ResumeVersion;
import com.anzs.module.resume.mapper.ResumeMapper;
import com.anzs.module.resume.mapper.ResumeTemplateMapper;
import com.anzs.module.resume.mapper.ResumeVersionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeMapper resumeMapper;
    private final ResumeTemplateMapper resumeTemplateMapper;
    private final ResumeVersionMapper resumeVersionMapper;

    // === Template ===

    public List<ResumeTemplateVO> templateList(Integer type) {
        LambdaQueryWrapper<ResumeTemplate> qw = new LambdaQueryWrapper<>();
        qw.eq(ResumeTemplate::getStatus, 0);
        if (type != null) qw.eq(ResumeTemplate::getType, type);
        qw.orderByDesc(ResumeTemplate::getCreatedAt);
        return resumeTemplateMapper.selectList(qw).stream()
                .map(this::toTemplateVO)
                .collect(Collectors.toList());
    }

    // === Resume CRUD ===

    @Transactional
    public Map<String, Object> createResume(Long userId, ResumeSaveDTO dto) {
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setTemplateId(dto.getTemplateId());
        resume.setTitle(dto.getTitle());
        resume.setContentJson(dto.getContentJson());
        resume.setVersion(1);
        resume.setCreatedAt(LocalDateTime.now());
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.insert(resume);

        ResumeVersion version = createVersion(resume.getId(), 1, dto.getTemplateId(), dto.getContentJson());
        resume.setCurrentVersionId(version.getId());
        resumeMapper.updateById(resume);

        return Map.of("resumeId", String.valueOf(resume.getId()), "version", 1);
    }

    @Transactional
    public Map<String, Object> updateResume(Long userId, Long resumeId, ResumeSaveDTO dto) {
        Resume resume = validateOwnership(userId, resumeId);
        int nextVersionNo = resume.getVersion() + 1;

        ResumeVersion version = createVersion(resumeId, nextVersionNo, dto.getTemplateId(), dto.getContentJson());

        resume.setTitle(dto.getTitle());
        resume.setTemplateId(dto.getTemplateId());
        resume.setContentJson(dto.getContentJson());
        resume.setVersion(nextVersionNo);
        resume.setCurrentVersionId(version.getId());
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.updateById(resume);

        return Map.of("resumeId", String.valueOf(resumeId), "version", nextVersionNo);
    }

    public IPage<ResumeVO> getMyResumes(Long userId, int page, int size) {
        Page<Resume> p = new Page<>(page, size);
        LambdaQueryWrapper<Resume> qw = new LambdaQueryWrapper<>();
        qw.eq(Resume::getUserId, userId);
        qw.orderByDesc(Resume::getUpdatedAt);
        IPage<Resume> result = resumeMapper.selectPage(p, qw);

        Map<Long, String> templateNameCache = new HashMap<>();
        return result.convert(r -> toResumeVO(r, templateNameCache));
    }

    public ResumeDetailVO getResumeDetail(Long userId, Long resumeId) {
        Resume resume = validateOwnership(userId, resumeId);
        Map<Long, String> cache = new HashMap<>();
        ResumeVO base = toResumeVO(resume, cache);
        ResumeDetailVO detail = new ResumeDetailVO();
        copyFields(base, detail);
        detail.setContentJson(resume.getContentJson());
        return detail;
    }

    @Transactional
    public Map<String, Object> duplicateResume(Long userId, Long sourceResumeId) {
        Resume source = validateOwnership(userId, sourceResumeId);
        Resume copy = new Resume();
        copy.setUserId(userId);
        copy.setTemplateId(source.getTemplateId());
        copy.setTitle(source.getTitle() + " (副本)");
        copy.setContentJson(source.getContentJson());
        copy.setVersion(1);
        copy.setCreatedAt(LocalDateTime.now());
        copy.setUpdatedAt(LocalDateTime.now());
        resumeMapper.insert(copy);

        ResumeVersion version = createVersion(copy.getId(), 1, source.getTemplateId(), source.getContentJson());
        copy.setCurrentVersionId(version.getId());
        resumeMapper.updateById(copy);

        return Map.of("resumeId", String.valueOf(copy.getId()), "version", 1);
    }

    // === Version History ===

    public List<ResumeVersionVO> getVersions(Long userId, Long resumeId) {
        validateOwnership(userId, resumeId);
        LambdaQueryWrapper<ResumeVersion> qw = new LambdaQueryWrapper<>();
        qw.eq(ResumeVersion::getResumeId, resumeId);
        qw.orderByDesc(ResumeVersion::getVersionNo);
        return resumeVersionMapper.selectList(qw).stream()
                .map(v -> {
                    ResumeVersionVO vo = new ResumeVersionVO();
                    vo.setId(v.getId());
                    vo.setResumeId(v.getResumeId());
                    vo.setVersionNo(v.getVersionNo());
                    vo.setTemplateId(v.getTemplateId());
                    vo.setCreatedAt(v.getCreatedAt());
                    ResumeTemplate t = resumeTemplateMapper.selectById(v.getTemplateId());
                    vo.setTemplateName(t != null ? t.getName() : null);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    public ResumeVersionDetailVO getVersionDetail(Long userId, Long resumeId, Long versionId) {
        validateOwnership(userId, resumeId);
        ResumeVersion version = resumeVersionMapper.selectById(versionId);
        if (version == null || !version.getResumeId().equals(resumeId)) {
            throw new BizException("版本不存在");
        }
        ResumeVersionDetailVO vo = new ResumeVersionDetailVO();
        vo.setId(version.getId());
        vo.setResumeId(version.getResumeId());
        vo.setVersionNo(version.getVersionNo());
        vo.setTemplateId(version.getTemplateId());
        vo.setContentJson(version.getContentJson());
        vo.setCreatedAt(version.getCreatedAt());
        ResumeTemplate t = resumeTemplateMapper.selectById(version.getTemplateId());
        vo.setTemplateName(t != null ? t.getName() : null);
        return vo;
    }

    @Transactional
    public Map<String, Object> restoreVersion(Long userId, Long resumeId, Long versionId) {
        Resume resume = validateOwnership(userId, resumeId);
        ResumeVersion oldVersion = resumeVersionMapper.selectById(versionId);
        if (oldVersion == null || !oldVersion.getResumeId().equals(resumeId)) {
            throw new BizException("版本不存在");
        }
        int nextVersionNo = resume.getVersion() + 1;
        ResumeVersion newVersion = createVersion(resumeId, nextVersionNo,
                oldVersion.getTemplateId(), oldVersion.getContentJson());

        resume.setTemplateId(oldVersion.getTemplateId());
        resume.setContentJson(oldVersion.getContentJson());
        resume.setVersion(nextVersionNo);
        resume.setCurrentVersionId(newVersion.getId());
        resume.setUpdatedAt(LocalDateTime.now());
        resumeMapper.updateById(resume);

        return Map.of("resumeId", String.valueOf(resumeId), "version", nextVersionNo);
    }

    // === Helpers ===

    private Resume validateOwnership(Long userId, Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw new BizException("简历不存在或无权限");
        }
        return resume;
    }

    private ResumeVersion createVersion(Long resumeId, int versionNo, Long templateId, String contentJson) {
        ResumeVersion v = new ResumeVersion();
        v.setResumeId(resumeId);
        v.setVersionNo(versionNo);
        v.setTemplateId(templateId);
        v.setContentJson(contentJson);
        v.setCreatedAt(LocalDateTime.now());
        resumeVersionMapper.insert(v);
        return v;
    }

    private ResumeTemplateVO toTemplateVO(ResumeTemplate t) {
        ResumeTemplateVO vo = new ResumeTemplateVO();
        vo.setId(t.getId());
        vo.setName(t.getName());
        vo.setType(t.getType());
        vo.setThumbnailUrl(t.getThumbnailUrl());
        vo.setStructureJson(t.getStructureJson());
        vo.setCreatedAt(t.getCreatedAt());
        return vo;
    }

    private ResumeVO toResumeVO(Resume r, Map<Long, String> templateNameCache) {
        ResumeVO vo = new ResumeVO();
        vo.setId(r.getId());
        vo.setUserId(r.getUserId());
        vo.setTemplateId(r.getTemplateId());
        vo.setTitle(r.getTitle());
        vo.setVersion(r.getVersion());
        vo.setCurrentVersionId(r.getCurrentVersionId());
        vo.setCreatedAt(r.getCreatedAt());
        vo.setUpdatedAt(r.getUpdatedAt());
        String name = templateNameCache.computeIfAbsent(r.getTemplateId(), tid -> {
            ResumeTemplate t = resumeTemplateMapper.selectById(tid);
            return t != null ? t.getName() : null;
        });
        vo.setTemplateName(name);
        return vo;
    }

    private void copyFields(ResumeVO src, ResumeVO dst) {
        dst.setId(src.getId());
        dst.setUserId(src.getUserId());
        dst.setTemplateId(src.getTemplateId());
        dst.setTemplateName(src.getTemplateName());
        dst.setTitle(src.getTitle());
        dst.setVersion(src.getVersion());
        dst.setCurrentVersionId(src.getCurrentVersionId());
        dst.setCreatedAt(src.getCreatedAt());
        dst.setUpdatedAt(src.getUpdatedAt());
    }
}
