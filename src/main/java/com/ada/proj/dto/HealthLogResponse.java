package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthLogResponse {
    @Schema(example = "UP")
    private String status;
    private Instant lastLogAt;
    private int warnCount;
    private int errorCount;
    private List<String> lastLines;
}
