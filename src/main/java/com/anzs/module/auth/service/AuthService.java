package com.anzs.module.auth.service;

import com.anzs.common.exception.BizException;
import com.anzs.common.util.JwtUtil;
import com.anzs.infrastructure.cache.RedisCache;
import com.anzs.infrastructure.mail.EmailService;
import com.anzs.module.auth.dto.*;
import com.anzs.module.user.entity.SysUser;
import com.anzs.module.user.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final RedisCache redisCache;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    public void sendVerifyCode(VerifyCodeDTO dto) {
        String limitKey = "verify:limit:" + dto.getEmail();
        if (redisCache.hasKey(limitKey)) {
            throw new BizException("请稍后再试");
        }
        String code = String.format("%06d", (int) (Math.random() * 1000000));
        redisCache.set("verify:" + dto.getEmail() + ":" + dto.getScene(), code, 5, TimeUnit.MINUTES);
        redisCache.set(limitKey, "1", 60, TimeUnit.SECONDS);
        emailService.sendVerifyCode(dto.getEmail(), code, dto.getScene());
    }

    @Transactional
    public Map<String, Object> register(RegisterDTO dto) {
        String key = "verify:" + dto.getEmail() + ":REGISTER";
        String cached = redisCache.get(key);
        if (cached == null || !cached.equals(dto.getVerifyCode())) {
            throw new BizException("验证码错误或已过期");
        }
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, dto.getEmail())
        );
        if (count > 0) {
            throw new BizException("该邮箱已注册，请直接登录");
        }
        SysUser user = new SysUser();
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole(dto.getRole());
//        user.setStudentEmail(dto.getStudentEmail());
        user.setNickname(dto.getEmail().split("@")[0]);
        user.setStatus(0);
        user.setPointsBalance(20); // 注册赠送初始积分
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.insert(user);
        redisCache.delete(key);

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        Map<String, Object> map = new HashMap<>();
        map.put("userId", user.getId());
        map.put("accessToken", accessToken);
        map.put("refreshToken", refreshToken);
        map.put("expiresIn", accessTokenExpireMs());
        return map;
    }

    public Map<String, Object> login(LoginDTO dto) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, dto.getEmail())
        );
        if (user == null) {
            throw new BizException("账号不存在，请先注册");
        }
        if (user.getStatus() == 1) {
            throw new BizException("账号已被禁用，请联系管理员");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BizException("密码错误");
        }
        user.setLastLoginAt(LocalDateTime.now());
        sysUserMapper.updateById(user);

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        Map<String, Object> map = new HashMap<>();
        map.put("userId", user.getId());
        map.put("accessToken", accessToken);
        map.put("refreshToken", refreshToken);
        map.put("expiresIn", accessTokenExpireMs());
        return map;
    }

    public Map<String, Object> refresh(String refreshToken) {
        try {
            io.jsonwebtoken.Claims claims = jwtUtil.parseToken(refreshToken);
            if (!"REFRESH".equals(claims.get("type"))) {
                throw new BizException("无效的刷新令牌");
            }
            Long userId = Long.valueOf(claims.getSubject());
            SysUser user = sysUserMapper.selectById(userId);
            if (user == null || user.getStatus() == 1) {
                throw new BizException("账号不存在或已被禁用");
            }
            String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
            String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());
            Map<String, Object> map = new HashMap<>();
            map.put("accessToken", newAccessToken);
            map.put("refreshToken", newRefreshToken);
            map.put("expiresIn", accessTokenExpireMs());
            return map;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("刷新令牌无效或已过期");
        }
    }

    private Long accessTokenExpireMs() {
        return jwtUtil.getAccessTokenExpireMs();
    }

    @Transactional
    public void resetPassword(ResetPasswordDTO dto) {
        String key = "verify:" + dto.getEmail() + ":RESET_PASSWORD";
        String cached = redisCache.get(key);
        if (cached == null || !cached.equals(dto.getVerifyCode())) {
            throw new BizException("验证码错误或已过期");
        }
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, dto.getEmail())
        );
        if (user == null) {
            throw new BizException("账号不存在");
        }
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
        redisCache.delete(key);
    }
}
