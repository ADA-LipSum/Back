package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PollVoterResponse", description = "투표자 응답")
public class PollVoterResponse {

    @Schema(description = "투표자 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String voterUuid;

    @Schema(description = "투표자 표시 이름", example = "홍길동")
    private String voterName;
}
