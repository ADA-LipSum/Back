package com.ada.proj.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.ada.proj.dto.CommentCreateRequest;
import com.ada.proj.dto.CommentResponse;
import com.ada.proj.dto.CommentUpdateRequest;
import com.ada.proj.dto.ApiResponse;
import com.ada.proj.service.CommentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Tag(name = "댓글", description = "댓글 작성/조회/수정/삭제 및 좋아요·고정 기능 API")
public class CommentController {

    private final CommentService commentService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/posts/{postUuid}/comments")
    @Operation(
            summary = "댓글 작성",
            description = """
                    게시물에 댓글을 작성합니다. 로그인이 필요합니다.

                    **Path Variable:**
                    - `postUuid` (필수): 댓글을 달 게시글 UUID

                    **Request Body:**
                    - `content` (필수): 댓글 내용
                    - `parentId` (선택): 부모 댓글 ID — 대댓글인 경우에만 포함

                    **Response:**
                    - `id`: 생성된 댓글 ID
                    - `postId`: 게시글 UUID
                    - `parentId`: 부모 댓글 ID (대댓글인 경우)
                    - `content`: 댓글 내용
                    - `writerUuid`: 작성자 UUID
                    - `likes`: 좋아요 수 (초기 0)
                    - `pinned`: 고정 여부 (초기 false)
                    - `createdAt`: 작성 시각
                    """
    )
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @Parameter(description = "게시글 UUID") @PathVariable("postId") String postId,
            @RequestBody @Valid CommentCreateRequest request
    ) {
        CommentCreateRequest payload = Objects.requireNonNull(request, "request");
        payload.setPostId(postId); // PathVariable로 덮어쓰기
        return ResponseEntity.ok(ApiResponse.success(commentService.createComment(payload)));
    }

    @GetMapping("/posts/{postUuid}/comments")
    @Operation(
            summary = "댓글 목록 조회",
            description = """
                    게시글에 달린 댓글 및 대댓글 전체를 조회합니다.

                    **Path Variable:**
                    - `postUuid` (필수): 게시글 UUID

                    **Response:** 댓글 목록 배열 (id, postId, parentId, content, writerUuid, likes, pinned, createdAt)
                    """
    )
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @Parameter(description = "게시글 UUID") @PathVariable("postId") String postId
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getCommentsByPost(postId)));
    }

    @PutMapping("/{commentId}")
    @Operation(
            summary = "댓글 수정",
            description = """
                    본인이 작성한 댓글을 수정합니다.

                    **Path Variable:**
                    - `commentId` (필수): 수정할 댓글 ID

                    **Request Body:**
                    - `content` (필수): 수정할 댓글 내용

                    **Response:** 수정된 댓글 정보 (id, content, updatedAt 등)

                    본인이 작성하지 않은 댓글은 403을 반환합니다.
                    """
    )
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest req
    ) {
        Long id = requireCommentId(commentId);
        CommentUpdateRequest payload = Objects.requireNonNull(req, "request");
        return ResponseEntity.ok(ApiResponse.success(commentService.updateComment(id, payload)));
    }

    @DeleteMapping("/{commentId}")
    @Operation(
            summary = "댓글 삭제",
            description = """
                    본인이 작성한 댓글을 삭제합니다.

                    **Path Variable:**
                    - `commentId` (필수): 삭제할 댓글 ID

                    **Request Body:** 없음

                    **Response:** 성공 응답 (data: null)

                    본인이 작성하지 않은 댓글은 403을 반환합니다.
                    """
    )
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(requireCommentId(commentId));
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PutMapping("/{commentId}/like")
    @Operation(
            summary = "댓글 좋아요",
            description = """
                    댓글에 좋아요를 추가합니다.

                    **Path Variable:**
                    - `commentId` (필수): 좋아요를 추가할 댓글 ID

                    **Request Body:** 없음

                    **Response:**
                    - `likes`: 현재 좋아요 수
                    """
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> addLike(
            @PathVariable Long commentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentService.addLike(requireCommentId(commentId))));
    }

    @DeleteMapping("/{commentId}/like")
    @Operation(
            summary = "댓글 좋아요 해제",
            description = """
                    댓글 좋아요를 취소합니다.

                    **Path Variable:**
                    - `commentId` (필수): 좋아요를 취소할 댓글 ID

                    **Request Body:** 없음

                    **Response:**
                    - `likes`: 현재 좋아요 수
                    """
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeLike(
            @PathVariable Long commentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentService.removeLike(requireCommentId(commentId))));
    }

    @PutMapping("/{commentId}/pin")
    @Operation(
            summary = "댓글 고정",
            description = """
                    댓글을 고정합니다. 게시글 작성자만 사용 가능합니다.

                    **Path Variable:**
                    - `commentId` (필수): 고정할 댓글 ID

                    **Request Body:** 없음

                    **Response:**
                    - `pinned`: 고정 여부 (true)
                    """
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> pinComment(
            @PathVariable Long commentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentService.pinComment(requireCommentId(commentId))));
    }

    @DeleteMapping("/{commentId}/pin")
    @Operation(
            summary = "댓글 고정 해제",
            description = """
                    댓글 고정을 해제합니다. 게시글 작성자만 사용 가능합니다.

                    **Path Variable:**
                    - `commentId` (필수): 고정 해제할 댓글 ID

                    **Request Body:** 없음

                    **Response:**
                    - `pinned`: 고정 여부 (false)
                    """
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> unpinComment(
            @PathVariable Long commentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentService.unpinComment(requireCommentId(commentId))));
    }

    private @NonNull
    String requireUuid(String postUuid) {
        return Objects.requireNonNull(postUuid, "postUuid");
    }

    private @NonNull
    Long requireCommentId(Long commentId) {
        return Objects.requireNonNull(commentId, "commentId");
    }
}
