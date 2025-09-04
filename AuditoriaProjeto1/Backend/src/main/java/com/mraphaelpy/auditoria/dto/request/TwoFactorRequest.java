package com.mraphaelpy.auditoria.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TwoFactorRequest {
    
    @NotBlank(message = "Email é obrigatório")
    private String email;
    
    @NotBlank(message = "Código é obrigatório")
    @Pattern(regexp = "^[0-9]{6}$", message = "Código deve conter exatamente 6 dígitos")
    private String code;
    
    @NotBlank(message = "Token de sessão é obrigatório")
    private String sessionToken;
}
