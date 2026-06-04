package com.ada.proj.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PopularTagResponse", description = "인기 개발 태그 항목")
public class PopularTagResponse {

    @Schema(description = "태그 이름", example = "React")
    private String tag;

    @Schema(description = "게시글 작성 시 이 태그가 사용된 횟수", example = "42")
    private long count;
}
