package com.anzs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${shore.upload.path}")
    private String uploadPath;

    @Value("${shore.pdf.temp-dir:./temp/pdf}")
    private String pdfTempDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(uploadPath);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadDir.toUri().toString());
        Path pdfDir = Paths.get(pdfTempDir).toAbsolutePath();
        registry.addResourceHandler("/exports/**")
                .addResourceLocations(pdfDir.toUri().toString());
    }
}
