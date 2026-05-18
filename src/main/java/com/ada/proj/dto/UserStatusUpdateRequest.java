package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "사용자 활성 상태 변경 요청")
public class UserStatusUpdateRequest {

    @NotNull
    @Schema(description = "활성 여부 (true: 활성화, false: 비활성화)", example = "false")
    private Boolean active;
}
