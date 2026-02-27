package cn.har01d.survey.dto.auth;

import lombok.Data;

import java.time.Instant;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
    private String role;
    private Instant createdAt;
}
