package com.anzs.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordDTO {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String verifyCode;

    @NotBlank
    private String newPassword;
}
