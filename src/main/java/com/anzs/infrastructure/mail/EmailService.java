package com.anzs.infrastructure.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    public void sendVerifyCode(String email, String code, String scene) {
        log.info("[邮件模拟发送] 邮箱: {}, 场景: {}, 验证码: {}", email, scene, code);
        // TODO: 接入真实邮件服务（JavaMail / SendGrid / 阿里云邮件推送）
    }
}
