package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "프로젝트 순위 선정 요청")
public class ProjectSelectionRequest {

    @NotNull
    @Schema(description = "우수 프로젝트(순위) 선정 여부", example = "true")
    private Boolean selected;
}
