package com.anzs.module.auth.controller;

import com.anzs.common.Result;
import com.anzs.module.auth.dto.*;
import com.anzs.module.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/verify-code")
    public Result<Void> sendVerifyCode(@RequestBody @Valid VerifyCodeDTO dto) {
        authService.sendVerifyCode(dto);
        return Result.ok();
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody @Valid RegisterDTO dto) {
        return Result.ok(authService.register(dto));
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody @Valid LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(@RequestBody @Valid RefreshTokenDTO dto) {
        return Result.ok(authService.refresh(dto.getRefreshToken()));
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody @Valid ResetPasswordDTO dto) {
        authService.resetPassword(dto);
        return Result.ok();
    }
}
