package com.ada.proj.controller;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.ada.proj.dto.PageResponse;
import com.ada.proj.dto.PostCreateRequest;
import com.ada.proj.dto.PostDetailResponse;
import com.ada.proj.dto.PostSummaryResponse;
import com.ada.proj.dto.PostUpdateRequest;
import com.ada.proj.enums.PostBoardType;
import com.ada.proj.service.PostService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/blog/posts")
@Tag(name = "블로그", description = "블로그 게시글 목록/검색/작성/상세/수정/삭제 API")
public class BlogController {

    private final PostService postService;

    @GetMapping
    @Operation(
            summary = "블로그 게시글 목록 조회 및 검색",
            description = """
                    블로그 게시글을 최신순으로 조회합니다. 기술 태그와 검색어로 필터링할 수 있습니다.

                    **Query Parameters:**
                    - `page` (선택): 페이지 번호, 0부터 시작
                    - `size` (선택): 페이지 크기
                    - `techTag` (선택): Spring, React, MySQL 같은 기술 태그
                    - `query` (선택): 제목/본문 검색어

                    **Response:**
                    - `data.content`: 블로그 게시글 요약 목록
                    - `thumbnailImage`: 본문 첫 번째 이미지에서 자동 추출된 썸네일 URL
                    - `techTags`: 선택된 기술 태그 목록
                    - `likes`, `views`, `comments`: 좋아요/방문/댓글 수
                    """
    )
    public ResponseEntity<ApiResponse<PageResponse<PostSummaryResponse>>> list(
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "기술 태그 필터", example = "Spring")
            @RequestParam(required = false) String techTag,
            @Parameter(description = "제목/본문 검색어", example = "JPA")
            @RequestParam(required = false) String query
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.search(
                PostBoardType.BLOG,
                null,
                null,
                techTag,
                query,
                page,
                size
        )));
    }

    @PostMapping
    @Operation(
            summary = "블로그 게시글 작성",
            description = """
                    블로그 게시글을 작성합니다. 로그인이 필요합니다.

                    **Request Body:**
                    - `title` (필수): 제목, 최대 20자
                    - `content` (선택): 본문. Markdown 이미지 또는 HTML 이미지 포함 가능
                    - `techTags` (선택): 기술 태그 목록, 다중 선택 가능
                    - `thumbnailImage` (선택): 명시 썸네일 URL

                    **썸네일 자동 설정:**
                    - `thumbnailImage`를 직접 전달하면 해당 값을 사용합니다.
                    - 비어 있으면 본문 내 첫 번째 Markdown 이미지 또는 HTML 이미지의 URL을 자동 추출합니다.

                    **Response:**
                    - `data`: 생성된 게시글 순번 (Long)
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
                .body(ApiResponse.success(postService.createBlog(payload)));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "블로그 게시글 상세 조회",
            description = """
                    블로그 게시글 상세 정보를 조회합니다. 조회 시 방문 수가 1 증가합니다.

                    **Path Variable:**
                    - `id` (필수): 게시글 순번

                    **Response:**
                    - 제목, 본문, 작성자 정보
                    - 썸네일, 기술 태그
                    - 좋아요 수, 방문 수, 댓글 수
                    - `isLiked`: 현재 로그인 사용자의 좋아요 여부 (비로그인 시 false)
                    """
    )
    public ResponseEntity<ApiResponse<PostDetailResponse>> detail(
            @Parameter(description = "게시글 순번", example = "1") @PathVariable Long id,
            Authentication authentication
    ) {
        String requesterUuid = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(ApiResponse.success(postService.detail(id, requesterUuid)));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "블로그 게시글 수정",
            description = """
                    블로그 게시글을 수정합니다. 작성자 본인 또는 관리자만 가능합니다.

                    **Path Variable:**
                    - `id` (필수): 수정할 게시글 순번

                    **Request Body:**
                    - `title`: 제목
                    - `content`: 본문
                    - `techTags`: 기술 태그 목록
                    - `thumbnailImage`: 썸네일 URL. 비우고 본문이 변경되면 첫 번째 이미지로 다시 계산됩니다.

                    **Response:** 성공 응답
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> update(
            @Parameter(description = "게시글 순번", example = "1") @PathVariable Long id,
            @Valid @RequestBody PostUpdateRequest request,
            Authentication authentication
    ) {
        postService.updateBlog(id, request, authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "블로그 게시글 삭제",
            description = """
                    블로그 게시글을 삭제합니다. 작성자 본인 또는 관리자만 가능합니다.

                    **Path Variable:**
                    - `id` (필수): 삭제할 게시글 순번
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "게시글 순번", example = "1") @PathVariable Long id,
            Authentication authentication
    ) {
        postService.delete(id, authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
