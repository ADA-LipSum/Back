package com.ada.proj.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.ErrorCode;
import com.ada.proj.exception.ForbiddenException;
import com.ada.proj.exception.InvalidCredentialsException;
import com.ada.proj.exception.TokenExpiredException;
import com.ada.proj.exception.TokenInvalidException;
import com.ada.proj.exception.UnauthenticatedException;
import com.ada.proj.exception.UserNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException e, HttpServletRequest req) {
        logWarn(e, req, 400, ErrorCode.BAD_REQUEST.name());
        return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.BAD_REQUEST.name(), e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(IllegalStateException e, HttpServletRequest req) {
        logWarn(e, req, 409, ErrorCode.CONFLICT.name());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ErrorCode.CONFLICT.name(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e, HttpServletRequest req) {
        String msg = e.getBindingResult().getAllErrors().stream().findFirst().map(err -> err.getDefaultMessage()).orElse("Validation error");
        logWarn(e, req, 400, ErrorCode.VALIDATION_ERROR.name());
        return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.VALIDATION_ERROR.name(), msg));
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthenticated(UnauthenticatedException e, HttpServletRequest req) {
        logWarn(e, req, 401, ErrorCode.UNAUTHENTICATED.name());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ErrorCode.UNAUTHENTICATED.name(), e.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException e, HttpServletRequest req) {
        logWarn(e, req, 403, ErrorCode.FORBIDDEN.name());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ErrorCode.FORBIDDEN.name(), e.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(UserNotFoundException e, HttpServletRequest req) {
        logWarn(e, req, 404, ErrorCode.USER_NOT_FOUND.name());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ErrorCode.USER_NOT_FOUND.name(), e.getMessage()));
    }

    @ExceptionHandler({InvalidCredentialsException.class, TokenInvalidException.class, TokenExpiredException.class})
    public ResponseEntity<ApiResponse<Void>> handleAuthFailures(RuntimeException e, HttpServletRequest req) {
        // InvalidCredentials -> INVALID_PASSWORD, TokenExpired -> TOKEN_EXPIRED, TokenInvalid -> TOKEN_INVALID
        String code =
            e instanceof InvalidCredentialsException ? ErrorCode.INVALID_PASSWORD.name() :
            e instanceof TokenExpiredException ? ErrorCode.TOKEN_EXPIRED.name() :
            e instanceof TokenInvalidException ? ErrorCode.TOKEN_INVALID.name() : ErrorCode.AUTH_FAILURE.name();
        logWarn(e, req, 401, code);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(code, e.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurity(SecurityException e, HttpServletRequest req) {
        // fallback for generic SecurityException -> 403
        logWarn(e, req, 403, ErrorCode.FORBIDDEN.name());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ErrorCode.FORBIDDEN.name(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception e, HttpServletRequest req) {
        logError(e, req, 500, ErrorCode.INTERNAL_ERROR.name());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.name(), "Internal server error"));
    }

    private void logWarn(Exception e, HttpServletRequest req, int status, String code) {
        String rid = MDC.get("requestId");
        String path = req != null ? req.getRequestURI() : "";
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class)
                .warn("에러 요청: id={} code={} status={} path={} msg={}", rid, code, status, path, e.getMessage());
    }

    private void logError(Exception e, HttpServletRequest req, int status, String code) {
        String rid = MDC.get("requestId");
        String path = req != null ? req.getRequestURI() : "";
        log.error("서버 오류: id={} code={} status={} path={}", rid, code, status, path, e);
    }
}
