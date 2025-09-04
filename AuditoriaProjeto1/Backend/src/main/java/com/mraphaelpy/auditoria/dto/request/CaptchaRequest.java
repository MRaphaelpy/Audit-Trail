package com.mraphaelpy.auditoria.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CaptchaRequest {
    @NotBlank(message = "Email é obrigatório")
    private String email;

    @NotBlank(message = "Código é obrigatório")
    private String captcha;
}
