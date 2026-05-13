package com.anzs.module.resume.service;

import com.anzs.module.resume.entity.ResumeExport;
import com.anzs.module.resume.mapper.ResumeExportMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfExportCleanupService {

    private final ResumeExportMapper resumeExportMapper;

    @Value("${shore.pdf.temp-dir:./temp/pdf}")
    private String pdfTempDir;

    @Value("${shore.pdf.cleanup.retention-days:7}")
    private int retentionDays;

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredExports() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        LambdaQueryWrapper<ResumeExport> qw = new LambdaQueryWrapper<>();
        qw.lt(ResumeExport::getCreatedAt, cutoff);
        List<ResumeExport> expired = resumeExportMapper.selectList(qw);

        int deleted = 0;
        for (ResumeExport export : expired) {
            if (export.getPdfUrl() != null) {
                String fileName = export.getPdfUrl().substring(export.getPdfUrl().lastIndexOf('/') + 1);
                File pdfFile = new File(pdfTempDir, fileName);
                if (pdfFile.exists() && !pdfFile.delete()) {
                    log.warn("Failed to delete PDF: {}", pdfFile.getAbsolutePath());
                }
            }
            resumeExportMapper.deleteById(export.getId());
            deleted++;
        }
        log.info("Cleaned {} expired PDF exports (>{} days)", deleted, retentionDays);
    }
}
