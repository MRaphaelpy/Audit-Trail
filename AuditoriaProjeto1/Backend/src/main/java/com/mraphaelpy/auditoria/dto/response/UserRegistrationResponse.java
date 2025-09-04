package com.mraphaelpy.auditoria.dto.response;

import com.mraphaelpy.auditoria.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationResponse {

    private Long id;
    private String username;
    private String email;
    private LocalDateTime createdAt;

    public static UserRegistrationResponse from(User user) {
        if (user == null) {
            return null;
        }

        return UserRegistrationResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
