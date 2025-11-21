package com.ada.proj.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 통일된 API 응답 포맷.
 * 성공: {"success":true, "data":..., "message":"요청이 성공적으로 처리되었습니다."}
 * 실패: {"success":false, "errorCode":"USER_NOT_FOUND", "message":"해당 유저를 찾을 수 없습니다."}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final boolean success;
    private final T data; // 성공 시 반환 데이터
    private final String message; // 성공 또는 실패 메시지
    private final String errorCode; // 실패 시 에러 코드

    private ApiResponse(boolean success, T data, String message, String errorCode) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.errorCode = errorCode;
    }

    // 성공 (데이터 + 기본 메시지)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, "요청이 성공적으로 처리되었습니다.", null);
    }

    // 성공 (데이터 없이)
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(true, null, "요청이 성공적으로 처리되었습니다.", null);
    }

    // 커스텀 메시지 성공
    public static <T> ApiResponse<T> successMessage(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    public static ApiResponse<Void> successMessage(String message) {
        return new ApiResponse<>(true, null, message, null);
    }

    // 실패 (명시적 에러 코드)
    public static ApiResponse<Void> error(String errorCode, String message) {
        return new ApiResponse<>(false, null, message, errorCode);
    }

    // 기존 코드 호환: fail(message) -> ERROR 코드 사용
    public static ApiResponse<Void> fail(String message) {
        return new ApiResponse<>(false, null, message, "ERROR");
    }

    // 실패 (데이터 없음, 커스텀 코드/메시지)
    public static <T> ApiResponse<T> errorWithData(String errorCode, String message, T data) {
        return new ApiResponse<>(false, data, message, errorCode);
    }

    // 약식 별칭 (기존 일부 컨트롤러에서 사용중인 ok/okMessage 패턴 유지)
    public static <T> ApiResponse<T> ok(T data) { return success(data); }
    public static ApiResponse<Void> okMessage(String message) { return successMessage(message); }

    // getters (Lombok 제거로 수동 구현)
    public boolean isSuccess() { return success; }
    public T getData() { return data; }
    public String getMessage() { return message; }
    public String getErrorCode() { return errorCode; }
}
