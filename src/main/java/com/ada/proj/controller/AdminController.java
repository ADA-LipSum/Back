package com.ada.proj.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.service.AutoIncrementMaintenanceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@Tag(name = "관리자 도구", description = "자동 증가 재정렬 등 관리자 유지보수 작업 API")
public class AdminController {

    private final AutoIncrementMaintenanceService aiService;

    @PostMapping("/resequence/{table}")
    @Operation(
            summary = "자동 증가 재정렬",
            description = """
                    특정 테이블의 AUTO_INCREMENT 값을 재정렬합니다. ADMIN만 가능합니다.

                    **Path Variable:**
                    - `table` (필수): 재정렬할 테이블 이름 (예: posts, comments)

                    **Request Body:** 없음

                    **Response:** 성공 메시지 반환 (재정렬 시작 알림)

                    ADMIN이 아닌 경우 403을 반환합니다.
                    """
    )
    public ResponseEntity<ApiResponse<?>> resequenceTable(@PathVariable("table") String table, Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("로그인이 필요합니다."));
        }
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(403).body(ApiResponse.fail("관리자 권한이 필요합니다."));
        }

        aiService.triggerResequencePrimary(table);
        return ResponseEntity.ok(ApiResponse.success("resequence started for: " + table));
    }
}
