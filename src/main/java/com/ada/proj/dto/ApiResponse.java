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
public class ApiResponse<T> {
    @Schema(example = "true")
    private boolean success;
    private T data;
    private String message;

    // 데이터 포함 성공 응답
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("OK")
                .data(data)
                .build();
    }

    // 데이터 없는 성공 응답 (에러 해결)
    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
                .success(true)
                .message("OK")
                .data(null)
                .build();
    }

    // 단순 OK (data만 있는 경우)
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder().success(true).data(data).build();
    }

    // 단순 OK 메시지
    public static ApiResponse<Void> okMessage(String message) {
        return ApiResponse.<Void>builder().success(true).message(message).build();
    }

    // 실패 응답
    public static <T> ApiResponse<T> fail(String message) {
        return ApiResponse.<T>builder().success(false).message(message).build();
    }
}