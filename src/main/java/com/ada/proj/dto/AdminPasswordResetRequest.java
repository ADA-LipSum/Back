package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "비밀번호 강제 초기화 요청")
public class AdminPasswordResetRequest {

    @NotBlank
    @Size(min = 6, max = 255, message = "비밀번호는 6자 이상이어야 합니다")
    @Schema(description = "새 비밀번호 (6자 이상)", example = "NewP@ssw0rd!")
    private String newPassword;
}
