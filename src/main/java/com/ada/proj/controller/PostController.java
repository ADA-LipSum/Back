package com.ada.proj.controller;

import java.util.Objects;

import com.ada.proj.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.service.PostService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
@Tag(name = "게시물", description = "게시물 CRUD, 좋아요 등 게시판 기능 API")
public class PostController {

    private final PostService postService;
    private final com.ada.proj.service.CommentService commentService; // 댓글 조회 REST 경로 제공

    @PostMapping
    @Operation(
            summary = "게시물 생성",
            description = """
                    새 게시물을 작성합니다. 로그인이 필요합니다.

                    **Request Body:**
                    - `title` (필수): 게시글 제목 (최대 20자)
                    - `content`: 게시글 본문 (Markdown 형식, `contentMd` 별칭도 허용)
                    - `isDev`: 개발 관련 게시글 여부 (boolean)
                    - `devTags`: 개발 태그 (문자열, 쉼표 구분 등 자유 포맷)

                    **Response:**
                    - `data`: 생성된 게시글 UUID (String)

                    성공 시 HTTP 201을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<String>> create(
            @Valid @RequestBody PostCreateRequest data,
            Authentication authentication
    ) {
        PostCreateRequest payload = Objects.requireNonNull(data, "data");
        if (authentication != null) {
            payload.setWriterUuid(authentication.getName());
        }

        String uuid = postService.create(payload);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.success(uuid));
    }

    @PutMapping("/{uuid}")
    @Operation(
            summary = "게시글 수정",
            description = """
                    기존 게시글을 수정합니다. 작성자 본인 또는 ADMIN만 가능합니다.

                    **Path Variable:**
                    - `uuid` (필수): 수정할 게시글 UUID

                    **Request Body (모두 선택 — 포함된 필드만 업데이트):**
                    - `title`: 게시글 제목 (최대 20자)
                    - `content`: 본문 내용 (`contentMd` 별칭도 허용)
                    - `isDev`: 개발 관련 게시글 여부 (boolean)
                    - `devTags`: 개발 태그 문자열

                    **Response:** 성공 응답 (data: null)

                    본인이 아닌 경우 403을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> update(
            @Parameter(description = "게시글 UUID", example = "post-uuid-...")
            @PathVariable("uuid") String uuid,
            @RequestBody PostUpdateRequest req,
            Authentication authentication) {
        postService.update(requireUuid(uuid), requireUpdateRequest(req), authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{uuid}")
    @Operation(
            summary = "게시글 삭제",
            description = """
                    게시글을 삭제합니다. 작성자 본인 또는 ADMIN만 가능합니다.

                    **Path Variable:**
                    - `uuid` (필수): 삭제할 게시글 UUID

                    **Request Body:** 없음

                    **Response:** 성공 응답 (data: null)

                    본인이 아닌 경우 403을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "게시글 UUID", example = "post-uuid-...")
            @PathVariable("uuid") String uuid,
            Authentication authentication) {
        postService.delete(requireUuid(uuid), authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/{uuid}")
    @Operation(
            summary = "게시글 상세 조회",
            description = """
                    게시글 상세 정보를 조회합니다. 조회 시 조회수가 1 증가합니다.

                    **Path Variable:**
                    - `uuid` (필수): 조회할 게시글 UUID

                    **Response:**
                    - `uuid`: 게시글 UUID
                    - `title`: 제목
                    - `content`: 본문 (Markdown)
                    - `writerUuid`: 작성자 UUID
                    - `isDev`: 개발 관련 여부
                    - `devTags`: 개발 태그
                    - `likes`: 좋아요 수
                    - `views`: 조회수
                    - `createdAt`: 작성 시각
                    """
    )
    public ResponseEntity<ApiResponse<PostDetailResponse>> detail(
            @Parameter(description = "게시글 UUID", example = "post-uuid-...")
            @PathVariable("uuid") String uuid) {
        return ResponseEntity.ok(ApiResponse.success(postService.detail(requireUuid(uuid))));
    }

    @GetMapping
    @Operation(
            summary = "게시글 목록 조회",
            description = """
                    게시글 목록을 페이징하여 조회합니다.

                    **Query Parameters:**
                    - `page` (선택): 페이지 번호, 0부터 시작 (기본값: 0)
                    - `size` (선택): 페이지당 게시글 수 (기본값: 20)

                    **Response:**
                    - `page`: 현재 페이지 번호
                    - `size`: 페이지 크기
                    - `totalElements`: 전체 게시글 수
                    - `totalPages`: 전체 페이지 수
                    - `content`: 게시글 요약 목록 (uuid, title, writerUuid, likes, views, createdAt 등)
                    """
    )
    public ResponseEntity<ApiResponse<PageResponse<PostSummaryResponse>>> list(
            @Parameter(description = "조회할 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "한 페이지에 포함될 게시글 개수", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.list(page, size)));
    }

    @PostMapping("/{uuid}/like")
    @Operation(
            summary = "게시글 좋아요 토글",
            description = """
                    게시글 좋아요를 토글합니다. 로그인이 필요합니다.

                    **Path Variable:**
                    - `uuid` (필수): 대상 게시글 UUID

                    **Request Body:** 없음

                    **Response:**
                    - `data`: 현재 좋아요 상태 (true: 좋아요 추가됨, false: 좋아요 취소됨)

                    인증되지 않은 요청은 403을 반환합니다.
                    """
    )
    public ResponseEntity<ApiResponse<Boolean>> toggleLike(
            @PathVariable("uuid") String uuid,
            Authentication auth
    ) {
        if (auth == null) {
            throw new SecurityException("로그인이 필요합니다.");
        }
        String principal = Objects.requireNonNull(auth.getName(), "principal");
        boolean liked = postService.toggleLike(principal, requireUuid(uuid));
        return ResponseEntity.ok(ApiResponse.success(liked));
    }

    @DeleteMapping("/likes/{id}")
    @Operation(
            summary = "좋아요 삭제 (id)",
            description = """
                    좋아요 고유 ID로 좋아요를 삭제합니다. 본인이 누른 좋아요만 삭제 가능합니다.

                    **Path Variable:**
                    - `id` (필수): 삭제할 좋아요의 숫자 ID

                    **Request Body:** 없음

                    **Response:** 성공 응답 (data: null)

                    본인의 좋아요가 아닌 경우 403을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> deleteLikeById(
            @Parameter(description = "좋아요 id", example = "1")
            @PathVariable("id") Long id,
            Authentication auth
    ) {
        if (auth == null) {
            throw new SecurityException("로그인이 필요합니다.");
        }
        String principal = Objects.requireNonNull(auth.getName(), "principal");
        postService.deleteLikeById(requireLikeId(id), principal);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/{uuid}/comments")
    @Operation(
            summary = "게시글 댓글 조회",
            description = """
                    게시글에 달린 댓글 및 대댓글 전체를 조회합니다.

                    **Path Variable:**
                    - `uuid` (필수): 게시글 UUID

                    **Response:** 댓글 목록 배열
                    - `id`: 댓글 ID
                    - `postId`: 게시글 UUID
                    - `parentId`: 부모 댓글 ID (대댓글인 경우)
                    - `content`: 댓글 내용
                    - `writerUuid`: 작성자 UUID
                    - `likes`: 좋아요 수
                    - `pinned`: 고정 여부
                    - `createdAt`: 작성 시각
                    """
    )
    public ResponseEntity<ApiResponse<java.util.List<CommentResponse>>> comments(
            @Parameter(description = "게시글 UUID") @PathVariable("postUuid") String postUuid) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getCommentsByPost(requireUuid(postUuid))));
    }

    private @NonNull
    String requireUuid(String uuid) {
        return Objects.requireNonNull(uuid, "uuid");
    }

    private @NonNull
    Long requireLikeId(Long id) {
        return Objects.requireNonNull(id, "id");
    }

    private @NonNull
    PostUpdateRequest requireUpdateRequest(PostUpdateRequest req) {
        return Objects.requireNonNull(req, "request");
    }
}
