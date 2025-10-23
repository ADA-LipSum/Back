package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @Schema(description = "관리자 발급 ID 또는 커스텀 ID")
    @NotBlank
    private String id;

    @NotBlank
    private String password;
}
