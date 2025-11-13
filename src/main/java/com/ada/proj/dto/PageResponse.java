package com.ada.proj.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageResponse<T> {
    @Schema(description = "현재 페이지 (0부터 시작)")
    private int page;
    @Schema(description = "페이지 크기")
    private int size;
    @Schema(description = "전체 건수")
    private long totalElements;
    @Schema(description = "전체 페이지 수")
    private int totalPages;
    @Schema(description = "내용")
    private List<T> content;
}
