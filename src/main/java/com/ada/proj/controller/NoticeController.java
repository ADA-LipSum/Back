package com.ada.proj.controller;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.NoticeSummaryResponse;
import com.ada.proj.dto.PageResponse;
import com.ada.proj.dto.PostCreateRequest;
import com.ada.proj.dto.PostDetailResponse;
import com.ada.proj.dto.PostUpdateRequest;
import com.ada.proj.enums.NoticeCategory;
import com.ada.proj.service.PostService;

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
@Tag(name = "공지사항", description = "공지사항 CRUD, 핀 고정/해제(📌), 분류 필터, 키워드 검색, 파일 첨부, 북마크 — 작성/수정/삭제/핀은 관리자 전용")
public class NoticeController {

    private final PostService postService;

    @GetMapping
    @Operation(
            summary = "공지사항 목록 조회",
            description = """
                    공지사항 목록을 반환합니다.

                    **정렬 규칙**
                    1. 📌 고정 게시물 → `pinnedAt` 최신순
                    2. 일반 게시물 → `writedAt` 최신순

                    **Response 주요 필드**
                    | 필드 | 설명 |
                    |---|---|
                    | seq | 번호 (고정 게시물은 UI에서 📌 표시 권장) |
                    | noticeCategory | EVENT · SERVICE · OTHER |
                    | isPinned | 고정 여부 |
                    | views | 누적 조회수 |
                    """
    )
    public ResponseEntity<ApiResponse<PageResponse<NoticeSummaryResponse>>> list(
            @Parameter(description = "페이지 번호 (0부터)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "분류 필터", schema = @Schema(allowableValues = {"EVENT", "SERVICE", "OTHER"}))
            @RequestParam(required = false) String category,
            @Parameter(description = "제목·내용 키워드 검색어", example = "점검")
            @RequestParam(required = false) String query
    ) {
        NoticeCategory noticeCategory = (category != null && !category.isBlank()) ? NoticeCategory.from(category) : null;
        return ResponseEntity.ok(ApiResponse.success(
                postService.listNotices(page, size, noticeCategory, query)));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "공지사항 상세 조회",
            description = "공지사항 상세를 반환합니다. 호출 시 **조회수 +1**. `attachments` 필드에 첨부파일 목록(다운로드 URL 포함)이 반환됩니다."
    )
    public ResponseEntity<ApiResponse<PostDetailResponse>> detail(
            @Parameter(description = "공지사항 게시글 순번") @PathVariable Long id,
            Authentication authentication
    ) {
        String requesterUuid = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(ApiResponse.success(postService.detail(id, requesterUuid)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(
            summary = "공지사항 작성 (관리자)",
            description = """
                    공지사항을 작성합니다. **관리자 전용.**

                    | 필드 | 필수 | 설명 |
                    |---|---|---|
                    | title | ✅ | 제목 (최대 20자) |
                    | content | - | 본문 |
                    | noticeCategory | - | `EVENT`(행사) · `SERVICE`(서비스) · `OTHER`(기타) |
                    | attachmentIds | - | 먼저 `POST /api/upload/notice/attachment` 로 업로드 후 반환된 ID 목록 |

                    **Response:** `data` = 생성된 공지사항 순번(Long)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Long>> create(
            @Valid @RequestBody PostCreateRequest request,
            Authentication authentication
    ) {
        PostCreateRequest payload = Objects.requireNonNull(request, "request");
        if (authentication != null) {
            payload.setWriterUuid(authentication.getName());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(postService.createNotice(payload)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(
            summary = "공지사항 수정 (관리자)",
            description = "공지사항을 수정합니다. **관리자 전용.** 전달한 필드만 부분 업데이트됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> update(
            @Parameter(description = "공지사항 게시글 순번") @PathVariable Long id,
            @Valid @RequestBody PostUpdateRequest request,
            Authentication authentication
    ) {
        postService.updateNotice(id, request, authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(
            summary = "공지사항 삭제 (관리자)",
            description = "공지사항을 삭제합니다. **관리자 전용.** 첨부파일 연결 및 이모지 반응이 함께 삭제됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "공지사항 게시글 순번") @PathVariable Long id,
            Authentication authentication
    ) {
        postService.delete(id, authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/pin")
    @Operation(
            summary = "공지사항 고정 (관리자)",
            description = "공지사항을 목록 최상단에 고정합니다. **관리자 전용.** 다중 고정 지원. `pinnedAt` 최신순으로 정렬됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> pin(
            @Parameter(description = "공지사항 게시글 순번") @PathVariable Long id,
            Authentication authentication
    ) {
        postService.pinNotice(id, authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/pin")
    @Operation(
            summary = "공지사항 고정 해제 (관리자)",
            description = "공지사항 고정을 해제합니다. **관리자 전용.**",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> unpin(
            @Parameter(description = "공지사항 게시글 순번") @PathVariable Long id,
            Authentication authentication
    ) {
        postService.unpinNotice(id, authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{id}/bookmark")
    @Operation(
            summary = "북마크 토글",
            description = "공지사항 북마크를 추가하거나 제거합니다. **로그인 필요.** `data` = `true`(추가) / `false`(제거)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Boolean>> toggleBookmark(
            @Parameter(description = "공지사항 게시글 순번") @PathVariable Long id,
            Authentication authentication
    ) {
        if (authentication == null) throw new SecurityException("로그인이 필요합니다.");
        boolean bookmarked = postService.toggleBookmark(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success(bookmarked));
    }
}
