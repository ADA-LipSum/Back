package com.ada.proj.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.CommunityEventCreateRequest;
import com.ada.proj.dto.CommunityEventResponse;
import com.ada.proj.dto.CommunityEventUpdateRequest;
import com.ada.proj.service.CommunityEventService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/widget/events")
@Tag(name = "이벤트 위젯", description = "커뮤니티 다가오는 이벤트 위젯 — 조회는 비로그인 허용, 생성·수정·삭제는 ADMIN/TEACHER 전용")
public class CommunityEventController {

    private final CommunityEventService eventService;

    @GetMapping
    @Operation(
            summary = "활성 이벤트 목록 조회",
            description = """
                    종료일이 오늘 이후인 이벤트를 시작일 오름차순으로 반환합니다. 로그인 불필요.

                    - 모든 이벤트가 종료된 경우 빈 배열 `[]`을 반환합니다.
                    - 최대 6개의 이벤트가 등록될 수 있습니다.
                    """
    )
    public ResponseEntity<ApiResponse<List<CommunityEventResponse>>> getActiveEvents() {
        return ResponseEntity.ok(ApiResponse.success(eventService.getActiveEvents()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(
            summary = "전체 이벤트 목록 조회 (ADMIN/TEACHER)",
            description = "종료된 이벤트를 포함한 전체 목록을 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<List<CommunityEventResponse>>> getAllEvents() {
        return ResponseEntity.ok(ApiResponse.success(eventService.getAllEvents()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(
            summary = "이벤트 생성 (ADMIN/TEACHER)",
            description = """
                    이벤트를 등록합니다. **ADMIN 또는 TEACHER 전용.** `multipart/form-data`로 전송합니다.

                    | 파트 | 타입 | 필수 | 설명 |
                    |---|---|---|---|
                    | `request` | JSON | ✅ | 제목·시작일·종료일·장소·설명·관련링크 |
                    | `thumbnail` | 파일 | ❌ | 썸네일 이미지 |

                    - 이벤트는 최대 **6개**까지 등록 가능합니다.
                    - 종료일은 시작일보다 이전일 수 없습니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<CommunityEventResponse>> create(
            @Parameter(description = "이벤트 정보 (JSON)")
            @RequestPart("request") @Valid CommunityEventCreateRequest request,
            @Parameter(description = "썸네일 이미지 (선택)")
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @Parameter(hidden = true) Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(eventService.create(request, thumbnail, authentication.getName())));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(
            summary = "이벤트 수정 (ADMIN/TEACHER)",
            description = """
                    이벤트를 수정합니다. **ADMIN 또는 TEACHER 전용.** `multipart/form-data`로 전송합니다.

                    | 파트 | 타입 | 필수 | 설명 |
                    |---|---|---|---|
                    | `request` | JSON | ✅ | 수정할 필드 (null이면 변경 없음) |
                    | `thumbnail` | 파일 | ❌ | 새 썸네일 (전달하면 교체, 생략하면 기존 유지) |
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<CommunityEventResponse>> update(
            @Parameter(description = "이벤트 ID", example = "1") @PathVariable Long id,
            @Parameter(description = "수정 정보 (JSON)")
            @RequestPart("request") @Valid CommunityEventUpdateRequest request,
            @Parameter(description = "새 썸네일 이미지 (선택)")
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
    ) {
        return ResponseEntity.ok(ApiResponse.success(eventService.update(id, request, thumbnail)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    @Operation(
            summary = "이벤트 삭제 (ADMIN/TEACHER)",
            description = "이벤트를 삭제합니다. **ADMIN 또는 TEACHER 전용.**",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "이벤트 ID", example = "1") @PathVariable Long id
    ) {
        eventService.delete(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
