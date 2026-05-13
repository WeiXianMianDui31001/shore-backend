package com.anzs.module.resume.service;

import com.anzs.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeImageUploadService {

    private static final long MAX_BYTES = 3 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");

    @Value("${shore.upload.path}")
    private String uploadPath;

    @Value("${shore.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    /**
     * 证件照等：保存到本地 uploads，返回相对路径与绝对 URL（供 iframe 预览与 PDF 拉取）。
     */
    public Map<String, String> uploadResumePhoto(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择图片文件");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BizException("图片不能超过 3MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BizException("仅支持图片格式");
        }

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        }
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BizException("仅支持 jpg、png、webp、gif");
        }

        String fileName = UUID.randomUUID() + ext;
        Path dir = Paths.get(uploadPath, "resume", String.valueOf(userId));
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);
            file.transferTo(target.toFile());
        } catch (Exception e) {
            throw new BizException("图片保存失败: " + e.getMessage());
        }

        String relative = "/uploads/resume/" + userId + "/" + fileName;
        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        String absolute = base + relative;
        return Map.of("url", relative, "absoluteUrl", absolute);
    }
}
