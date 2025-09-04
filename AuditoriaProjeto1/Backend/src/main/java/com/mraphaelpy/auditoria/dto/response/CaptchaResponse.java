package com.mraphaelpy.auditoria.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResponse {

    private Boolean success;
    private String message;
    private String status;

    public static CaptchaResponse success(String message) {
        return CaptchaResponse.builder()
                .success(true)
                .message(message)
                .status("success")
                .build();
    }

    public static CaptchaResponse failure(String message) {
        return CaptchaResponse.builder()
                .success(false)
                .message(message)
                .status("failure")
                .build();
    }

    public static CaptchaResponse error(String message) {
        return CaptchaResponse.builder()
                .success(false)
                .message(message)
                .status("error")
                .build();
    }
}
