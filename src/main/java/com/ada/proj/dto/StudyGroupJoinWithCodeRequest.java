package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StudyGroupJoinWithCodeRequest {

    @NotBlank
    @Schema(description = "비공개 그룹 초대 코드 (6자리)", example = "AB12CD")
    private String code;
}
