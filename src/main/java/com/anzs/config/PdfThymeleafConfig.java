package com.anzs.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

@Configuration
public class PdfThymeleafConfig {

    private final ApplicationContext applicationContext;

    public PdfThymeleafConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean("pdfTemplateEngine")
    public SpringTemplateEngine pdfTemplateEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(pdfTemplateResolver());
        return engine;
    }

    private SpringResourceTemplateResolver pdfTemplateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setApplicationContext(applicationContext);
        resolver.setPrefix("classpath:/templates/resume/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(true);
        resolver.setCharacterEncoding("UTF-8");
        return resolver;
    }
}
