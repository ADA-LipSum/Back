package com.ada.proj.controller;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.enums.ErrorCode;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ApiResponse<Void>> handleError(HttpServletRequest request) {
        HttpStatus status = resolveStatus(request);
        String code = status == HttpStatus.NOT_FOUND
                ? ErrorCode.NOT_FOUND.name()
                : ErrorCode.INTERNAL_ERROR.name();
        String message = status == HttpStatus.NOT_FOUND ? "Not found" : "Internal server error";

        return ResponseEntity.status(status).body(ApiResponse.error(code, message));
    }

    private HttpStatus resolveStatus(HttpServletRequest request) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusCode instanceof Integer code) {
            return HttpStatus.resolve(code) == null
                    ? HttpStatus.INTERNAL_SERVER_ERROR
                    : HttpStatus.valueOf(code);
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
