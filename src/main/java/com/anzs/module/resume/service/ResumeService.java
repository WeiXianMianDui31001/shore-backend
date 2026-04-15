package com.anzs.module.resume.service;

import com.anzs.common.exception.BizException;
import com.anzs.module.resume.entity.Resume;
import com.anzs.module.resume.entity.ResumeExport;
import com.anzs.module.resume.entity.ResumeTemplate;
import com.anzs.module.resume.mapper.ResumeExportMapper;
import com.anzs.module.resume.mapper.ResumeMapper;
import com.anzs.module.resume.mapper.ResumeTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeMapper resumeMapper;
    private final ResumeTemplateMapper resumeTemplateMapper;
    private final ResumeExportMapper resumeExportMapper;

    public List<ResumeTemplate> templateList(Integer type) {
        LambdaQueryWrapper<ResumeTemplate> qw = new LambdaQueryWrapper<>();
        qw.eq(ResumeTemplate::getStatus, 0);
        if (type != null) qw.eq(ResumeTemplate::getType, type);
        qw.orderByDesc(ResumeTemplate::getCreatedAt);
        return resumeTemplateMapper.selectList(qw);
    }

    @Transactional
    public Map<String, Object> saveResume(Long userId, Resume resume) {
        resume.setUserId(userId);
        resume.setUpdatedAt(LocalDateTime.now());
        if (resume.getId() == null) {
            resume.setVersion(1);
            resume.setCreatedAt(LocalDateTime.now());
            resumeMapper.insert(resume);
        } else {
            Resume existing = resumeMapper.selectById(resume.getId());
            if (existing == null || !existing.getUserId().equals(userId)) {
                throw new BizException("简历不存在或无权限");
            }
            resume.setVersion(existing.getVersion() + 1);
            resumeMapper.updateById(resume);
        }
        return Map.of("resumeId", resume.getId(), "version", resume.getVersion());
    }

    public IPage<Resume> myResumes(Long userId, Integer page, Integer size) {
        Page<Resume> p = new Page<>(page, size);
        LambdaQueryWrapper<Resume> qw = new LambdaQueryWrapper<>();
        qw.eq(Resume::getUserId, userId);
        qw.orderByDesc(Resume::getUpdatedAt);
        return resumeMapper.selectPage(p, qw);
    }

    public Resume getResume(Long userId, Long id) {
        Resume resume = resumeMapper.selectById(id);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw new BizException("简历不存在或无权限");
        }
        return resume;
    }

    @Transactional
    public Map<String, Object> exportResume(Long userId, Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw new BizException("简历不存在或无权限");
        }
        ResumeExport export = new ResumeExport();
        export.setResumeId(resumeId);
        export.setCreatedAt(LocalDateTime.now());
        resumeExportMapper.insert(export);
        // 异步生成 PDF 逻辑可在此触发
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("exportId", export.getId());
        result.put("status", "PROCESSING");
        result.put("pdfUrl", null);
        return result;
    }

    public ResumeExport getExportStatus(Long exportId) {
        return resumeExportMapper.selectById(exportId);
    }
}
