package com.ada.proj.controller;

import java.util.Objects;

import com.ada.proj.dto.*;
import org.springframework.http.ResponseEntity;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
@Tag(name = "게시물", description = "게시물 CRUD, 좋아요 등 게시판 기능 API")
public class PostController {

    private final PostService postService;
    private final com.ada.proj.service.CommentService commentService;

    @PostMapping
    @Operation(
            summary = "게시글 작성",
            description = """
                    새 게시글을 작성합니다. **JWT 인증 필요.**

                    **Request Body:**
                    - `title` (필수): 게시글 제목 (최대 20자)
                    - `content` (선택): 게시글 본문 (Markdown 형식)
                    - `images` (선택): 이미지 URL 문자열
                    - `videos` (선택): 영상 URL 문자열
                    - `isDev` (선택): 개발 관련 게시글 여부 (boolean)
                    - `devTags` (선택): 개발 태그 (쉼표 구분 문자열)

                    **Response:**
                    - `data`: 생성된 게시글 순번 (Long)

                    성공 시 HTTP 201을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Long>> create(
            @Valid @RequestBody PostCreateRequest data,
            Authentication authentication
    ) {
        PostCreateRequest payload = Objects.requireNonNull(data, "data");
        if (authentication != null) {
            payload.setWriterUuid(authentication.getName());
        }

        Long seq = postService.create(payload);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.success(seq));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "게시글 수정",
            description = """
                    기존 게시글을 수정합니다. **JWT 인증 필요.** 작성자 본인 또는 ADMIN만 가능합니다.

                    **Path Variable:**
                    - `id` (필수): 수정할 게시글 순번

                    **Request Body (포함된 필드만 업데이트):**
                    - `title` (선택): 게시글 제목 (최대 20자)
                    - `content` (선택): 본문 내용 (Markdown 형식)
                    - `images` (선택): 이미지 URL 문자열
                    - `videos` (선택): 영상 URL 문자열
                    - `isDev` (선택): 개발 관련 게시글 여부 (boolean)
                    - `devTags` (선택): 개발 태그 문자열

                    **Response:** 성공 응답 (data: null)

                    본인이 아닌 경우 403을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> update(
            @Parameter(description = "게시글 순번", example = "1")
            @PathVariable Long id,
            @RequestBody PostUpdateRequest req,
            Authentication authentication) {
        postService.update(id, Objects.requireNonNull(req, "request"), authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "게시글 삭제",
            description = """
                    게시글을 삭제합니다. **JWT 인증 필요.** 작성자 본인 또는 ADMIN만 가능합니다.

                    **Path Variable:**
                    - `id` (필수): 삭제할 게시글 순번

                    **Response:** 성공 응답 (data: null)

                    본인이 아닌 경우 403을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "게시글 순번", example = "1")
            @PathVariable Long id,
            Authentication authentication) {
        postService.delete(id, authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "게시글 상세 조회",
            description = """
                    게시글 상세 정보를 조회합니다. 조회 시 조회수가 1 증가합니다. 인증 불필요.

                    **Path Variable:**
                    - `id` (필수): 조회할 게시글 순번

                    **Response:**
                    - `postUuid`: 게시글 UUID
                    - `seq`: 게시글 순번
                    - `writerUuid`: 작성자 UUID
                    - `writerCustomId`: 작성자 Custom ID
                    - `writer`: 작성자 이름
                    - `writerProfileImage`: 작성자 프로필 이미지 URL
                    - `title`: 게시글 제목
                    - `content`: 본문 (Markdown)
                    - `images`: 이미지 URL 문자열
                    - `videos`: 영상 URL 문자열
                    - `likes`: 좋아요 수
                    - `views`: 조회수
                    - `comments`: 댓글 수
                    - `isDev`: 개발 관련 여부
                    - `devTags`: 개발 태그
                    - `writedAt`: 작성 시각 (ISO 8601)
                    - `updatedAt`: 수정 시각 (ISO 8601)
                    - `isLiked`: 현재 로그인 사용자의 좋아요 여부 (비로그인 시 false)
                    """
    )
    public ResponseEntity<ApiResponse<PostDetailResponse>> detail(
            @Parameter(description = "게시글 순번", example = "1")
            @PathVariable Long id,
            Authentication auth) {
        String requesterUuid = (auth != null) ? auth.getName() : null;
        return ResponseEntity.ok(ApiResponse.success(postService.detail(id, requesterUuid)));
    }

    @GetMapping
    @Operation(
            summary = "게시글 목록 조회",
            description = """
                    게시글 목록을 페이징하여 최신순으로 조회합니다. 인증 불필요.

                    **Query Parameters:**
                    - `page` (선택): 페이지 번호, 0부터 시작 (기본값: 0)
                    - `size` (선택): 페이지당 게시글 수 (기본값: 20)

                    **Response:**
                    - `page`: 현재 페이지 번호
                    - `size`: 페이지 크기
                    - `totalElements`: 전체 게시글 수
                    - `totalPages`: 전체 페이지 수
                    - `content`: 게시글 요약 목록
                      - `postUuid`: 게시글 UUID
                      - `seq`: 게시글 순번
                      - `title`: 게시글 제목
                      - `writer`: 작성자 이름
                      - `writerProfileImage`: 작성자 프로필 이미지 URL
                      - `likes`: 좋아요 수
                      - `views`: 조회수
                      - `comments`: 댓글 수
                      - `isDev`: 개발 관련 여부
                      - `devTags`: 개발 태그
                      - `tag`: 분류 태그 (예: "개발(Java)", "일반")
                      - `writedAt`: 작성 시각 (ISO 8601)
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

    @PostMapping("/{id}/like")
    @Operation(
            summary = "게시글 좋아요 토글",
            description = """
                    게시글 좋아요를 토글합니다. **JWT 인증 필요.**

                    **Path Variable:**
                    - `id` (필수): 대상 게시글 순번

                    **Response:**
                    - `data`: 변경 후 좋아요 상태 (true: 좋아요 추가됨, false: 좋아요 취소됨)

                    인증되지 않은 요청은 403을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Boolean>> toggleLike(
            @Parameter(description = "게시글 순번", example = "1")
            @PathVariable Long id,
            Authentication auth
    ) {
        if (auth == null) {
            throw new SecurityException("로그인이 필요합니다.");
        }
        String principal = Objects.requireNonNull(auth.getName(), "principal");
        boolean liked = postService.toggleLike(principal, id);
        return ResponseEntity.ok(ApiResponse.success(liked));
    }

    @DeleteMapping("/likes/{likeId}")
    @Operation(
            summary = "좋아요 취소 (ID 기반)",
            description = """
                    좋아요 고유 ID로 좋아요를 취소합니다. **JWT 인증 필요.** 본인이 누른 좋아요만 취소 가능합니다.

                    **Path Variable:**
                    - `likeId` (필수): 취소할 좋아요의 숫자 ID

                    **Response:** 성공 응답 (data: null)

                    본인의 좋아요가 아닌 경우 403을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> deleteLikeById(
            @Parameter(description = "좋아요 id", example = "1")
            @PathVariable Long likeId,
            Authentication auth
    ) {
        if (auth == null) {
            throw new SecurityException("로그인이 필요합니다.");
        }
        String principal = Objects.requireNonNull(auth.getName(), "principal");
        postService.deleteLikeById(Objects.requireNonNull(likeId, "likeId"), principal);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/{id}/comments")
    @Operation(
            summary = "게시글 댓글 조회",
            description = """
                    게시글에 달린 댓글 및 대댓글 전체를 조회합니다. 인증 불필요.

                    **Path Variable:**
                    - `id` (필수): 게시글 순번

                    **Response:** 댓글 목록 배열
                    - `id`: 댓글 ID
                    - `postId`: 게시글 UUID
                    - `parentId`: 부모 댓글 ID (대댓글인 경우)
                    - `content`: 댓글 내용
                    - `writerUuid`: 작성자 UUID
                    - `likes`: 좋아요 수
                    - `pinned`: 고정 여부
                    - `createdAt`: 작성 시각 (ISO 8601)
                    """
    )
    public ResponseEntity<ApiResponse<java.util.List<CommentResponse>>> comments(
            @Parameter(description = "게시글 순번") @PathVariable Long id) {
        String postUuid = postService.findUuidBySeq(id);
        return ResponseEntity.ok(ApiResponse.success(commentService.getCommentsByPost(postUuid)));
    }
}
