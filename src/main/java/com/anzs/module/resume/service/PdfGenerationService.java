package com.anzs.module.resume.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.anzs.common.exception.BizException;
import com.anzs.module.resume.entity.Resume;
import com.anzs.module.resume.entity.ResumeExport;
import com.anzs.module.resume.entity.ResumeTemplate;
import com.anzs.module.resume.mapper.ResumeExportMapper;
import com.anzs.module.resume.mapper.ResumeMapper;
import com.anzs.module.resume.mapper.ResumeTemplateMapper;
import com.anzs.module.resume.mapper.ResumeVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfGenerationService {

    private final ResumeMapper resumeMapper;
    private final ResumeVersionMapper resumeVersionMapper;
    private final ResumeTemplateMapper resumeTemplateMapper;
    private final ResumeExportMapper resumeExportMapper;

    @Qualifier("pdfTemplateEngine")
    private final SpringTemplateEngine pdfTemplateEngine;

    @Value("${shore.pdf.temp-dir:./temp/pdf}")
    private String pdfTempDir;

    /** 解析 HTML 中 img 的 /uploads/... 相对地址（与 WebMvc 静态映射一致） */
    @Value("${shore.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    /** 与 {@code src/main/resources/static/resume/placeholder-avatar.png} 一致；data URL 便于 PDF 与 blob 预览同时加载 */
    private static final String PLACEHOLDER_AVATAR_RESOURCE = "static/resume/placeholder-avatar.png";

    private volatile String cachedPlaceholderAvatarDataUrl;

    @Async("pdfTaskExecutor")
    public void generatePdfAsync(Long exportId) {
        log.info("Async PDF generation started for export {}", exportId);
        try {
            generatePdf(exportId);
            log.info("Async PDF generation completed for export {}", exportId);
        } catch (Throwable e) {
            log.error("PDF generation failed for export {}: {}", exportId, e.getMessage(), e);
            try {
                ResumeExport export = resumeExportMapper.selectById(exportId);
                if (export != null) {
                    export.setStatus(2);
                    export.setErrorMessage(truncate(e.getMessage(), 500));
                    resumeExportMapper.updateById(export);
                }
            } catch (Exception ex) {
                log.error("Failed to update export status for {}", exportId, ex);
            }
        }
    }

    public void generatePdf(Long exportId) {
        ResumeExport export = resumeExportMapper.selectById(exportId);
        if (export == null) throw new BizException("导出记录不存在");

        Resume resume = resumeMapper.selectById(export.getResumeId());
        if (resume == null) throw new BizException("简历不存在");

        ResumeTemplate template;
        if (resume.getCurrentVersionId() != null) {
            var version = resumeVersionMapper.selectById(resume.getCurrentVersionId());
            template = version != null
                    ? resumeTemplateMapper.selectById(version.getTemplateId())
                    : resumeTemplateMapper.selectById(resume.getTemplateId());
        } else {
            template = resumeTemplateMapper.selectById(resume.getTemplateId());
        }
        if (template == null || template.getTemplateKey() == null) {
            throw new BizException("模板未配置 templateKey");
        }

        String html = renderHtml(resume, template);
        File pdfFile = generatePdfFile(html, exportId);
        String pdfUrl = "/exports/" + pdfFile.getName();

        export.setPdfUrl(pdfUrl);
        export.setFileSize(pdfFile.length());
        export.setStatus(1);
        resumeExportMapper.updateById(export);

        log.info("PDF generated: {} ({} bytes)", pdfUrl, pdfFile.length());
    }

    @SuppressWarnings("unchecked")
    private String renderHtml(Resume resume, ResumeTemplate template) {
        Context ctx = new Context();
        String contentJson = resume.getContentJson();
        if (contentJson == null || contentJson.isBlank()) {
            ctx.setVariable("sections", List.of());
            return pdfTemplateEngine.process(template.getTemplateKey(), ctx);
        }

        JSONObject root = JSONUtil.parseObj(contentJson);
        List<Map<String, Object>> sections = new ArrayList<>();

        JSONArray secArr = root.getJSONArray("sections");
        if (secArr != null) {
            for (int i = 0; i < secArr.size(); i++) {
                JSONObject sec = secArr.getJSONObject(i);
                Map<String, Object> secMap = new LinkedHashMap<>();
                secMap.put("key", sec.getStr("key"));
                secMap.put("title", sec.getStr("title"));
                String layoutOverride = sec.getStr("layout");
                if (layoutOverride != null && !layoutOverride.isBlank()) {
                    secMap.put("layout", layoutOverride);
                }

                List<Map<String, String>> items = new ArrayList<>();
                JSONArray itemArr = sec.getJSONArray("items");
                if (itemArr != null) {
                    for (int j = 0; j < itemArr.size(); j++) {
                        JSONObject item = itemArr.getJSONObject(j);
                        Map<String, String> fieldMap = new LinkedHashMap<>();
                        for (Map.Entry<String, Object> entry : item.entrySet()) {
                            fieldMap.put(entry.getKey(),
                                    entry.getValue() != null ? entry.getValue().toString() : "");
                        }
                        items.add(fieldMap);
                    }
                }
                secMap.put("items", items);
                sections.add(secMap);
            }
        }

        enrichSectionsForPdf(sections, template.getStructureJson(), template.getTemplateKey());
        ctx.setVariable("sections", sections);
        return pdfTemplateEngine.process(template.getTemplateKey(), ctx);
    }

    /**
     * 根据当前表单 JSON 渲染与导出一致的 HTML，供前端 iframe 实时预览（不落库、不写 PDF）。
     */
    public String previewHtml(Long templateId, String contentJson) {
        ResumeTemplate template = resumeTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BizException("模板不存在");
        }
        if (template.getTemplateKey() == null || template.getTemplateKey().isBlank()) {
            throw new BizException("模板未配置 templateKey");
        }
        Resume resume = new Resume();
        resume.setTemplateId(templateId);
        resume.setContentJson(contentJson);
        resume.setCurrentVersionId(null);
        return renderHtml(resume, template);
    }

    private File generatePdfFile(String html, Long exportId) {
        try {
            File pdfDir = new File(pdfTempDir);
            if (!pdfDir.exists()) pdfDir.mkdirs();
            File pdfFile = new File(pdfDir, "resume-" + exportId + ".pdf");

            try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
                ITextRenderer renderer = new ITextRenderer();

                // Try multiple possible font filenames
                URL fontUrl = getClass().getResource("/fonts/NotoSansCJKsc-Regular.otf");
                if (fontUrl == null) fontUrl = getClass().getResource("/fonts/NotoSansSC-Regular.ttf");
                if (fontUrl == null) fontUrl = getClass().getResource("/fonts/NotoSansSC-Regular.otf");
                if (fontUrl != null) {
                    renderer.getFontResolver().addFont(fontUrl.toExternalForm(),
                            "Noto Sans SC", "Identity-H", true, null);
                } else {
                    log.warn("Chinese font not found at /fonts/NotoSansSC-Regular.ttf");
                }

                String base = publicBaseUrl.trim();
                if (!base.endsWith("/")) {
                    base = base + "/";
                }
                renderer.setDocumentFromString(html, base);

                renderer.layout();
                renderer.createPDF(fos);
            }
            return pdfFile;
        } catch (Exception e) {
            throw new BizException("PDF生成失败: " + e.getMessage());
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    /**
     * 根据模板 structure_json 中的 layout / headlineFields 等元数据，为 PDF 渲染准备派生字段。
     * 支持的 layout：basic（仅标记）、kv（默认表格键值）、entry_headline_date、rich_entry。
     */
    @SuppressWarnings("unchecked")
    private void enrichSectionsForPdf(List<Map<String, Object>> sections, String structureJson, String templateKey) {
        Map<String, JSONObject> defByKey = parseStructureByKey(structureJson);
        for (Map<String, Object> sec : sections) {
            String key = (String) sec.get("key");
            JSONObject def = defByKey.get(key);

            String layout = (String) sec.get("layout");
            if (layout == null || layout.isBlank()) {
                layout = def != null ? def.getStr("layout") : null;
            }
            if (layout == null || layout.isBlank()) {
                layout = "basic".equals(key) ? "basic" : "kv";
            }
            sec.put("layout", layout);

            if ("basic".equals(key)) {
                if (usesSidePhotoHeader(templateKey) && def != null && basicSectionHasPhotoSlot(def)) {
                    applyPlaceholderAvatarIfMissing(sec);
                }
                continue;
            }

            Object rawItemsObj = sec.get("items");
            if (!(rawItemsObj instanceof List<?> rawList) || rawList.isEmpty()) {
                continue;
            }

            List<Map<String, Object>> displayItems = new ArrayList<>();
            for (Object o : rawList) {
                Map<String, Object> row = new LinkedHashMap<>();
                toStringMap(o).forEach(row::put);
                switch (layout) {
                    case "entry_headline_date" -> applyEntryHeadlineDate(row, def);
                    case "rich_entry" -> applyRichEntry(row, def);
                    default -> {
                    }
                }
                displayItems.add(row);
            }
            sec.put("items", displayItems);
        }
    }

    private Map<String, JSONObject> parseStructureByKey(String structureJson) {
        if (structureJson == null || structureJson.isBlank()) {
            return Map.of();
        }
        JSONArray arr = JSONUtil.parseArray(structureJson);
        Map<String, JSONObject> m = new LinkedHashMap<>();
        for (int i = 0; i < arr.size(); i++) {
            JSONObject o = arr.getJSONObject(i);
            String k = o.getStr("key");
            if (k != null && !k.isBlank()) {
                m.put(k, o);
            }
        }
        return m;
    }

    private boolean basicSectionHasPhotoSlot(JSONObject basicDef) {
        JSONArray arr = basicDef.getJSONArray("fields");
        if (arr == null || arr.isEmpty()) {
            return false;
        }
        for (int i = 0; i < arr.size(); i++) {
            String f = arr.getStr(i);
            if ("证件照".equals(f) || "照片".equals(f)) {
                return true;
            }
        }
        return false;
    }

    /** 与 Thymeleaf 片段 {@code page_body_cn_print}（右侧证件照）一致；居中表头模板不得注入，否则会当作文本展示。 */
    private static boolean usesSidePhotoHeader(String templateKey) {
        return "resume_cn_print_v1".equals(templateKey);
    }

    private Map<String, String> toStringMap(Object o) {
        Map<String, String> m = new LinkedHashMap<>();
        if (o instanceof Map<?, ?> om) {
            for (Map.Entry<?, ?> e : om.entrySet()) {
                m.put(String.valueOf(e.getKey()), e.getValue() != null ? String.valueOf(e.getValue()) : "");
            }
        }
        return m;
    }

    private void applyEntryHeadlineDate(Map<String, Object> row, JSONObject def) {
        List<String> hf = getStringList(def, "headlineFields", List.of("学校", "专业", "学历"));
        List<String> df = getStringList(def, "dateFields", List.of("入学时间", "毕业时间"));
        List<String> mf = getStringList(def, "moreFields", List.of());

        row.put("_headline", joinFieldValues(row, hf, "，"));

        List<String> dateParts = df.stream().map(f -> getStr(row, f)).filter(s -> !s.isBlank()).toList();
        String dateRange = switch (dateParts.size()) {
            case 0 -> "";
            case 1 -> dateParts.get(0);
            default -> dateParts.get(0) + " - " + dateParts.get(1);
        };
        row.put("_dateRange", dateRange);

        Set<String> reserved = new LinkedHashSet<>(hf);
        reserved.addAll(df);
        row.put("_extraRows", buildExtraRows(row, reserved, mf));
    }

    private void applyRichEntry(Map<String, Object> row, JSONObject def) {
        List<String> hf = getStringList(def, "headlineFields", List.of("项目名称"));
        List<String> df = getStringList(def, "dateFields", List.of());
        List<String> sf = getStringList(def, "sublineFields", List.of());
        List<String> bf = getStringList(def, "bodyFields", List.of("项目描述", "工作描述"));

        row.put("_headline", joinFieldValues(row, hf, "，"));

        List<String> dateParts = df.stream().map(f -> getStr(row, f)).filter(s -> !s.isBlank()).toList();
        String dateRange;
        if (!dateParts.isEmpty()) {
            dateRange = dateParts.size() == 1 ? dateParts.get(0) : dateParts.get(0) + " - " + dateParts.get(1);
        } else {
            dateRange = getStr(row, "起止时间");
        }
        row.put("_dateRange", dateRange);

        row.put("_subline", joinFieldValues(row, sf, " · "));

        String body = "";
        for (String f : bf) {
            String v = getStr(row, f);
            if (!v.isBlank()) {
                body = v;
                break;
            }
        }

        String bulletsRaw = getStr(row, "要点");
        List<String> bullets = new ArrayList<>();
        if (!bulletsRaw.isBlank()) {
            splitBulletLines(bulletsRaw, bullets);
            row.put("_body", body);
        } else if (body.contains("\n")) {
            String[] lines = body.split("\\r?\\n");
            row.put("_body", lines[0].trim());
            for (int i = 1; i < lines.length; i++) {
                String t = lines[i].trim();
                if (!t.isEmpty()) {
                    bullets.add(normalizeBullet(t));
                }
            }
        } else {
            row.put("_body", body);
        }
        row.put("_bullets", bullets);

        Set<String> reserved = new LinkedHashSet<>(hf);
        reserved.addAll(df);
        reserved.addAll(sf);
        reserved.addAll(bf);
        reserved.add("要点");
        row.put("_extraRows", buildExtraRows(row, reserved, List.of()));
    }

    private void splitBulletLines(String raw, List<String> bullets) {
        for (String line : raw.split("\\r?\\n")) {
            String t = line.trim();
            if (!t.isEmpty()) {
                bullets.add(normalizeBullet(t));
            }
        }
    }

    private String normalizeBullet(String t) {
        if (t.startsWith("•") || t.startsWith("-") || t.startsWith("·")) {
            return t;
        }
        return "• " + t;
    }

    private List<String> getStringList(JSONObject def, String name, List<String> defaults) {
        if (def == null) {
            return new ArrayList<>(defaults);
        }
        JSONArray arr = def.getJSONArray(name);
        if (arr == null || arr.isEmpty()) {
            return new ArrayList<>(defaults);
        }
        List<String> out = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            String s = arr.getStr(i);
            if (s != null && !s.isBlank()) {
                out.add(s);
            }
        }
        return out.isEmpty() ? new ArrayList<>(defaults) : out;
    }

    private String joinFieldValues(Map<String, Object> row, List<String> fields, String sep) {
        return fields.stream().map(f -> getStr(row, f)).filter(s -> !s.isBlank()).collect(Collectors.joining(sep));
    }

    private String getStr(Map<String, Object> row, String field) {
        Object v = row.get(field);
        if (v == null) {
            return "";
        }
        return String.valueOf(v).trim();
    }

    private List<Map<String, String>> buildExtraRows(Map<String, Object> row, Set<String> reservedKeys,
                                                     List<String> preferredMore) {
        List<Map<String, String>> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String k : preferredMore) {
            if (reservedKeys.contains(k)) {
                continue;
            }
            String v = getStr(row, k);
            if (!v.isBlank()) {
                out.add(kvRow(k, v));
                seen.add(k);
            }
        }
        for (Map.Entry<String, Object> e : row.entrySet()) {
            String k = e.getKey();
            if (k.startsWith("_") || reservedKeys.contains(k) || seen.contains(k)) {
                continue;
            }
            String v = e.getValue() != null ? String.valueOf(e.getValue()).trim() : "";
            if (!v.isBlank()) {
                out.add(kvRow(k, v));
            }
        }
        return out;
    }

    private Map<String, String> kvRow(String k, String v) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("k", k);
        m.put("v", v);
        return m;
    }

    /**
     * 侧栏证件照模板：未上传且未填链接时注入占位图（仅内存中的渲染数据，不写回用户保存的 JSON）。
     */
    @SuppressWarnings("unchecked")
    private void applyPlaceholderAvatarIfMissing(Map<String, Object> sec) {
        Object rawItemsObj = sec.get("items");
        if (!(rawItemsObj instanceof List<?> rawList) || rawList.isEmpty()) {
            return;
        }
        Object first = rawList.get(0);
        if (!(first instanceof Map<?, ?> row)) {
            return;
        }
        Map<String, Object> m = (Map<String, Object>) row;
        String idPhoto = stringField(m.get("证件照"));
        String photo = stringField(m.get("照片"));
        if (!idPhoto.isEmpty() || !photo.isEmpty()) {
            return;
        }
        String placeholder = resolvePlaceholderAvatarForRender();
        if (placeholder != null && !placeholder.isBlank()) {
            m.put("证件照", placeholder);
        }
    }

    private static String stringField(Object v) {
        if (v == null) {
            return "";
        }
        return String.valueOf(v).trim();
    }

    /**
     * 优先使用内嵌 PNG 的 data URL（PDF 引擎与 iframe blob 预览均无需再请求后端）；
     * 资源缺失时退回 {@link #publicBaseUrl} 下的静态文件地址。
     */
    private String resolvePlaceholderAvatarForRender() {
        String dataUrl = getPlaceholderAvatarDataUrl();
        if (dataUrl != null && !dataUrl.isBlank()) {
            return dataUrl;
        }
        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        if (base.isEmpty()) {
            base = "http://localhost:8080";
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/resume/placeholder-avatar.png";
    }

    private String getPlaceholderAvatarDataUrl() {
        if (cachedPlaceholderAvatarDataUrl != null) {
            return cachedPlaceholderAvatarDataUrl;
        }
        synchronized (this) {
            if (cachedPlaceholderAvatarDataUrl != null) {
                return cachedPlaceholderAvatarDataUrl;
            }
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = PdfGenerationService.class.getClassLoader();
            }
            try (InputStream in = cl.getResourceAsStream(PLACEHOLDER_AVATAR_RESOURCE)) {
                if (in == null) {
                    log.warn("Classpath resource missing: {}", PLACEHOLDER_AVATAR_RESOURCE);
                    cachedPlaceholderAvatarDataUrl = "";
                    return "";
                }
                byte[] bytes = in.readAllBytes();
                cachedPlaceholderAvatarDataUrl =
                        "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
            } catch (IOException e) {
                log.warn("Failed to read resume placeholder avatar: {}", e.getMessage());
                cachedPlaceholderAvatarDataUrl = "";
                return "";
            }
        }
        return cachedPlaceholderAvatarDataUrl;
    }
}
