package cn.har01d.survey.dto.auth;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String nickname;
    private String email;
    private String avatar;
    private String oldPassword;
    private String newPassword;
}
