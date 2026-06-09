package com.ada.proj.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.NoticeCreateRequest;
import com.ada.proj.dto.NoticeDetailResponse;
import com.ada.proj.dto.NoticeSummaryResponse;
import com.ada.proj.dto.NoticeUpdateRequest;
import com.ada.proj.dto.PageResponse;
import com.ada.proj.service.NoticeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
@Tag(name = "공지사항", description = "공지사항 CRUD, 고정/해제 (작성·수정·삭제·고정은 ADMIN 전용)")
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    @Operation(
            summary = "공지사항 목록 조회",
            description = """
                    공지사항 목록을 반환합니다. 인증 불필요.

                    **응답 순서**
                    1. 고정 게시물: `pinnedOrder` 오름차순
                    2. 일반 게시물: `seq` 내림차순 (페이징 적용)

                    **tag 필터** — 전달하지 않거나 `ALL`이면 전체 조회
                    | 값 | 설명 |
                    |---|---|
                    | EVENT | 행사 |
                    | SERVICE | 서비스 |
                    | EMPLOYMENT | 취업 |
                    | OTHER | 기타 |

                    > `totalElements` / `totalPages`는 일반 게시물(비고정)만 기준입니다.
                    """
    )
    public ResponseEntity<ApiResponse<PageResponse<NoticeSummaryResponse>>> list(
            @Parameter(description = "페이지 번호 (0부터)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "태그 필터 (ALL/EVENT/SERVICE/EMPLOYMENT/OTHER)",
                    schema = @Schema(allowableValues = {"ALL", "EVENT", "SERVICE", "EMPLOYMENT", "OTHER"}))
            @RequestParam(required = false, defaultValue = "ALL") String tag
    ) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.list(tag, page, size)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "공지사항 작성 (ADMIN)",
            description = """
                    공지사항을 작성합니다. **ADMIN 전용.** `multipart/form-data`로 전송합니다.

                    | 파트 | 타입 | 필수 | 설명 |
                    |---|---|---|---|
                    | `request` | JSON | ✅ | 제목·본문·태그 |
                    | `files` | 파일 | ❌ | 첨부파일 (여러 개 가능) |

                    **Response:** `data` = 생성된 게시물 순번(Long)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Long>> create(
            @Parameter(description = "공지사항 정보 (JSON)")
            @RequestPart("request") @Valid NoticeCreateRequest request,
            @Parameter(description = "첨부파일 목록 (선택)")
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @Parameter(hidden = true) Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(noticeService.create(request, files, authentication)));
    }

    @GetMapping("/{seq}")
    @Operation(
            summary = "공지사항 상세 조회",
            description = "공지사항 상세를 반환합니다. 호출 시 **조회수 +1** 됩니다. 인증 불필요."
    )
    public ResponseEntity<ApiResponse<NoticeDetailResponse>> detail(
            @Parameter(description = "게시물 순번", example = "1") @PathVariable Long seq
    ) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.detail(seq)));
    }

    @PutMapping(value = "/{seq}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "공지사항 수정 (ADMIN)",
            description = """
                    공지사항을 수정합니다. **ADMIN 전용.** `multipart/form-data`로 전송합니다.

                    | 파트 | 타입 | 필수 | 설명 |
                    |---|---|---|---|
                    | `request` | JSON | ✅ | 수정할 필드 (null이면 해당 필드 변경 없음) |
                    | `files` | 파일 | ❌ | 새 첨부파일 (전달하면 기존 첨부파일 전체 교체, 생략하면 유지) |
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> update(
            @Parameter(description = "게시물 순번", example = "1") @PathVariable Long seq,
            @Parameter(description = "수정 정보 (JSON)")
            @RequestPart("request") @Valid NoticeUpdateRequest request,
            @Parameter(description = "새 첨부파일 목록 (선택)")
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @Parameter(hidden = true) Authentication authentication
    ) {
        noticeService.update(seq, request, files, authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{seq}")
    @Operation(
            summary = "공지사항 삭제 (ADMIN)",
            description = "공지사항을 삭제합니다. **ADMIN 전용.** 첨부파일도 함께 삭제됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "게시물 순번", example = "1") @PathVariable Long seq,
            @Parameter(hidden = true) Authentication authentication
    ) {
        noticeService.delete(seq, authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{seq}/pin")
    @Operation(
            summary = "공지사항 고정 (ADMIN)",
            description = """
                    게시물을 목록 최상단에 고정합니다. **ADMIN 전용.**
                    이미 고정된 게시물이라면 변경 없이 성공 응답을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> pin(
            @Parameter(description = "게시물 순번", example = "1") @PathVariable Long seq,
            @Parameter(hidden = true) Authentication authentication
    ) {
        noticeService.pin(seq, authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{seq}/pin")
    @Operation(
            summary = "공지사항 고정 해제 (ADMIN)",
            description = "게시물의 고정을 해제합니다. **ADMIN 전용.**",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> unpin(
            @Parameter(description = "게시물 순번", example = "1") @PathVariable Long seq,
            @Parameter(hidden = true) Authentication authentication
    ) {
        noticeService.unpin(seq, authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
