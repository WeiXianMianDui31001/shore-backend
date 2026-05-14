package com.anzs.module.resume.service;

import com.anzs.common.exception.BizException;
import com.anzs.module.resume.dto.request.ResumeTemplateSaveDTO;
import com.anzs.module.resume.dto.response.ResumeTemplateAdminVO;
import com.anzs.module.resume.dto.response.ResumeTemplateVO;
import com.anzs.module.resume.entity.ResumeTemplate;
import com.anzs.module.resume.mapper.ResumeTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResumeTemplateService {

    private final ResumeTemplateMapper resumeTemplateMapper;

    public List<ResumeTemplateVO> listActiveTemplates(Integer type) {
        LambdaQueryWrapper<ResumeTemplate> qw = new LambdaQueryWrapper<>();
        qw.eq(ResumeTemplate::getStatus, 0);
        if (type != null) qw.eq(ResumeTemplate::getType, type);
        qw.orderByDesc(ResumeTemplate::getCreatedAt);
        return resumeTemplateMapper.selectList(qw).stream()
                .map(this::toTemplateVO)
                .collect(Collectors.toList());
    }

    public ResumeTemplateVO getTemplateDetail(Long id) {
        ResumeTemplate t = resumeTemplateMapper.selectById(id);
        if (t == null) throw new BizException("模板不存在");
        return toTemplateVO(t);
    }

    // === Admin ===

    public IPage<ResumeTemplateAdminVO> listAllTemplates(Integer type, Integer status, int page, int size) {
        Page<ResumeTemplate> p = new Page<>(page, size);
        LambdaQueryWrapper<ResumeTemplate> qw = new LambdaQueryWrapper<>();
        if (type != null) qw.eq(ResumeTemplate::getType, type);
        if (status != null) qw.eq(ResumeTemplate::getStatus, status);
        qw.orderByDesc(ResumeTemplate::getCreatedAt);
        return resumeTemplateMapper.selectPage(p, qw).convert(this::toAdminVO);
    }

    @Transactional
    public void createTemplate(ResumeTemplateSaveDTO dto) {
        ResumeTemplate t = new ResumeTemplate();
        t.setName(dto.getName());
        t.setType(dto.getType());
        t.setThumbnailUrl(dto.getThumbnailUrl());
        t.setStructureJson(dto.getStructureJson());
        t.setTemplateKey(dto.getTemplateKey());
        t.setTemplateVersion(dto.getTemplateVersion() != null ? dto.getTemplateVersion() : "v1");
        t.setStatus(dto.getStatus() != null ? dto.getStatus() : 0);
        t.setCreatedAt(LocalDateTime.now());
        resumeTemplateMapper.insert(t);
    }

    @Transactional
    public void updateTemplate(Long id, ResumeTemplateSaveDTO dto) {
        ResumeTemplate t = resumeTemplateMapper.selectById(id);
        if (t == null) throw new BizException("模板不存在");
        t.setName(dto.getName());
        t.setType(dto.getType());
        t.setThumbnailUrl(dto.getThumbnailUrl());
        t.setStructureJson(dto.getStructureJson());
        t.setTemplateKey(dto.getTemplateKey());
        t.setTemplateVersion(dto.getTemplateVersion() != null ? dto.getTemplateVersion() : "v1");
        if (dto.getStatus() != null) t.setStatus(dto.getStatus());
        resumeTemplateMapper.updateById(t);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        ResumeTemplate t = resumeTemplateMapper.selectById(id);
        if (t == null) throw new BizException("模板不存在");
        t.setStatus(1);
        resumeTemplateMapper.updateById(t);
    }

    public ResumeTemplate getTemplateEntity(Long id) {
        return resumeTemplateMapper.selectById(id);
    }

    // === Mapping ===

    private ResumeTemplateVO toTemplateVO(ResumeTemplate t) {
        ResumeTemplateVO vo = new ResumeTemplateVO();
        vo.setId(t.getId());
        vo.setName(t.getName());
        vo.setType(t.getType());
        vo.setThumbnailUrl(t.getThumbnailUrl());
        vo.setStructureJson(t.getStructureJson());
        vo.setTemplateKey(t.getTemplateKey());
        vo.setCreatedAt(t.getCreatedAt());
        return vo;
    }

    private ResumeTemplateAdminVO toAdminVO(ResumeTemplate t) {
        ResumeTemplateAdminVO vo = new ResumeTemplateAdminVO();
        vo.setId(t.getId());
        vo.setName(t.getName());
        vo.setType(t.getType());
        vo.setThumbnailUrl(t.getThumbnailUrl());
        vo.setStructureJson(t.getStructureJson());
        vo.setTemplateVersion(t.getTemplateVersion());
        vo.setStatus(t.getStatus());
        vo.setCreatedAt(t.getCreatedAt());
        return vo;
    }
}
