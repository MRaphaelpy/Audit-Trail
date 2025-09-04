package com.mraphaelpy.auditoria.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String message;
    private Boolean success;
    private String token;
    private String sessionToken;
    private Boolean requiresTwoFactor;
    private String username;
    private String captchaImage;
    private Boolean captchaEnabled;
    private Boolean twoFactorEnabled;
}
