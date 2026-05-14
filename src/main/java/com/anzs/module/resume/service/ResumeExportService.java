package com.anzs.module.resume.service;

import com.anzs.common.exception.BizException;
import com.anzs.module.resume.dto.response.ExportResultVO;
import com.anzs.module.resume.entity.Resume;
import com.anzs.module.resume.entity.ResumeExport;
import com.anzs.module.resume.mapper.ResumeExportMapper;
import com.anzs.module.resume.mapper.ResumeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ResumeExportService {

    private final ResumeExportMapper resumeExportMapper;
    private final ResumeMapper resumeMapper;
    private final PdfGenerationService pdfGenerationService;

    public ExportResultVO startExport(Long userId, Long resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null || !resume.getUserId().equals(userId)) {
            throw new BizException("简历不存在或无权限");
        }
        ResumeExport export = new ResumeExport();
        export.setResumeId(resumeId);
        export.setStatus(0);
        export.setCreatedAt(LocalDateTime.now());
        resumeExportMapper.insert(export);

        // MyBatis-Plus auto-commits, record is visible to async thread immediately
        pdfGenerationService.generatePdfAsync(export.getId());

        return toVO(resumeExportMapper.selectById(export.getId()));
    }

    public ExportResultVO getExportStatus(Long exportId) {
        ResumeExport export = resumeExportMapper.selectById(exportId);
        if (export == null) throw new BizException("导出记录不存在");
        return toVO(export);
    }

    private ExportResultVO toVO(ResumeExport export) {
        ExportResultVO vo = new ExportResultVO();
        vo.setExportId(export.getId());
        vo.setStatus(export.getStatus() == null ? "PROCESSING" :
                export.getStatus() == 1 ? "SUCCESS" :
                export.getStatus() == 2 ? "FAILED" : "PROCESSING");
        vo.setPdfUrl(export.getPdfUrl());
        vo.setFileSize(export.getFileSize());
        return vo;
    }
}
