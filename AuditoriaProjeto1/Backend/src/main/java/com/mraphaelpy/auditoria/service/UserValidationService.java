package com.mraphaelpy.auditoria.service;

import com.mraphaelpy.auditoria.entity.User;
import com.mraphaelpy.auditoria.repository.UserRepository;
import com.mraphaelpy.auditoria.service.audit.AuditoriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserValidationService {

    private final UserRepository userRepository;
    private final AuditoriaService auditoriaService;

    public User validateAndGetUser(String email, String ipAddress, String userAgent) {
        log.debug("Validando usuário para email: {}", email);

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            auditoriaService.loginFalha(email, ipAddress, userAgent, "Usuário não encontrado", 1);
            log.warn("Tentativa de login com email inexistente: {}", email);
            throw new UserNotFoundException("Usuário não encontrado");
        }

        User user = userOpt.get();
        log.debug("Usuário encontrado: {} (ID: {})", user.getUsername(), user.getId());

        return user;
    }

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }
}
