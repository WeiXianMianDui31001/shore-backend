package com.anzs.infrastructure.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    @Value("${spring.mail.username}")
    private String sysMailAddress;

    private final JavaMailSender mailSender;
    public void sendVerifyCode(String email, String code, String scene) {
        log.info("[邮件模拟发送] 邮箱: {}, 场景: {}, 验证码: {}", email, scene, code);
        // TODO: 接入真实邮件服务（JavaMail / SendGrid / 阿里云邮件推送）
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sysMailAddress);
        message.setTo(email);
        message.setSubject("[岸上见]验证码通知");
        message.setText("您的验证码是：" + code + "。5分钟内过期,请勿泄露给他人");
        mailSender.send(message);
    }
}
