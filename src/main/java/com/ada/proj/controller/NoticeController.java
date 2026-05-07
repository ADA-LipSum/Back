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
import com.ada.proj.service.PostService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
@Tag(name = "공지사항", description = "공지사항 전용 게시판 목록/상세/작성/수정/삭제 API")
public class NoticeController {

    private final PostService postService;

    @GetMapping
    @Operation(
            summary = "공지사항 목록 조회",
            description = """
                    공지사항 전용 게시판의 목록을 최신순으로 조회합니다.

                    **Query Parameters:**
                    - `page` (선택): 페이지 번호, 0부터 시작
                    - `size` (선택): 페이지 크기

                    **Response Fields:**
                    - `id`: 공지 번호. 게시글 UUID 값입니다.
                    - `title`: 제목
                    - `authorName`: 작성자 이름
                    - `authorProfileImage`: 작성자 프로필 이미지 URL
                    - `createdAt`: 등록일
                    """
    )
    public ResponseEntity<ApiResponse<PageResponse<NoticeSummaryResponse>>> list(
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.listNotices(page, size)));
    }

    @GetMapping("/{uuid}")
    @Operation(
            summary = "공지사항 상세 조회",
            description = """
                    공지사항 상세 정보를 조회합니다. 조회 시 조회수가 1 증가합니다.

                    **Path Variable:**
                    - `uuid` (필수): 공지사항 게시글 UUID

                    **Response:**
                    - 제목, 본문, 작성자 정보
                    - 등록일, 수정일, 조회 수
                    """
    )
    public ResponseEntity<ApiResponse<PostDetailResponse>> detail(
            @Parameter(description = "공지사항 게시글 UUID") @PathVariable String uuid
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.detail(uuid)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(
            summary = "공지사항 작성",
            description = """
                    공지사항을 작성합니다. 관리자 권한이 필요합니다.

                    **Request Body:**
                    - `title` (필수): 제목, 최대 20자
                    - `content` (선택): 공지 본문

                    요청의 `boardType` 값과 무관하게 `NOTICE` 게시글로 저장됩니다.

                    **Response:**
                    - `data`: 생성된 공지사항 게시글 UUID
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<String>> create(
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
    @PutMapping("/{uuid}")
    @Operation(
            summary = "공지사항 수정",
            description = """
                    공지사항을 수정합니다. 관리자 권한이 필요합니다.

                    **Path Variable:**
                    - `uuid` (필수): 수정할 공지사항 게시글 UUID

                    **Request Body:**
                    - `title`: 제목
                    - `content`: 본문

                    **Response:** 성공 응답
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> update(
            @Parameter(description = "공지사항 게시글 UUID") @PathVariable String uuid,
            @Valid @RequestBody PostUpdateRequest request,
            Authentication authentication
    ) {
        postService.updateNotice(uuid, request, authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{uuid}")
    @Operation(
            summary = "공지사항 삭제",
            description = """
                    공지사항을 삭제합니다. 관리자 권한이 필요합니다.

                    **Path Variable:**
                    - `uuid` (필수): 삭제할 공지사항 게시글 UUID
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "공지사항 게시글 UUID") @PathVariable String uuid,
            Authentication authentication
    ) {
        postService.delete(uuid, authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
