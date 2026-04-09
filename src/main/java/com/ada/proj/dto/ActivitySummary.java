package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ActivitySummary", description = "User activity summary")
public class ActivitySummary {

    @Schema(description = "Number of posts written")
    private long postCount;

    @Schema(description = "Number of comments written")
    private long commentCount;
}
