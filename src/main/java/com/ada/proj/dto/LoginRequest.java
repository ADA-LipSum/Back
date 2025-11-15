package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "LoginRequest", description = "로그인 요청 바디")
public class LoginRequest {
    @Schema(description = "관리자 발급 ID 또는 커스텀 ID", example = "teacher01")
    @NotBlank
    private String id;

    @Schema(description = "비밀번호", example = "P@ssw0rd!")
    @NotBlank
    private String password;
}
