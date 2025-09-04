package com.mraphaelpy.auditoria.controller;

import com.mraphaelpy.auditoria.dto.request.UserRegistrationRequest;
import com.mraphaelpy.auditoria.dto.response.ApiResponse;
import com.mraphaelpy.auditoria.dto.response.UserRegistrationResponse;
import com.mraphaelpy.auditoria.entity.User;
import com.mraphaelpy.auditoria.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        try {
            User user = userService.registerUser(request);

            UserRegistrationResponse userResponse = UserRegistrationResponse.from(user);

            return ResponseEntity.ok(ApiResponse.success("Usuário registrado com sucesso", userResponse));

        } catch (RuntimeException e) {
            log.warn("Erro ao registrar usuário: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Erro interno ao registrar usuário", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Erro interno do servidor"));
        }
    }
}
