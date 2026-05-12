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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Tag(name = "댓글", description = "댓글 작성/조회/수정/삭제 및 좋아요·고정 API")
public class CommentController {

    private final CommentService commentService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/posts/{postSeq}/comments")
    @Operation(
            summary = "댓글 작성",
            description = """
                    게시글에 댓글을 작성합니다. 로그인이 필요합니다.

                    **일반 댓글:** `content`만 전달
                    ```json
                    { "content": "댓글 내용" }
                    ```

                    **대댓글:** `parentId`에 부모 댓글 ID 추가
                    ```json
                    { "content": "대댓글 내용", "parentId": 1 }
                    ```

                    > `parentId`는 반드시 같은 게시글의 댓글 ID여야 합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @Parameter(description = "게시글 순번", example = "1") @PathVariable("postSeq") Long postSeq,
            @RequestBody @Valid CommentCreateRequest request
    ) {
        CommentCreateRequest payload = Objects.requireNonNull(request, "request");
        payload.setPostSeq(postSeq);
        return ResponseEntity.ok(ApiResponse.success(commentService.createComment(payload)));
    }

    @GetMapping("/posts/{postSeq}/comments")
    @Operation(
            summary = "댓글 목록 조회",
            description = "게시글에 달린 댓글 및 대댓글 전체를 조회합니다. 인증 불필요."
    )
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @Parameter(description = "게시글 순번", example = "1") @PathVariable("postSeq") Long postSeq
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getCommentsByPost(postSeq)));
    }

    @PutMapping("/{commentId}")
    @Operation(
            summary = "댓글 수정",
            description = """
                    본인이 작성한 댓글을 수정합니다. 본인이 아닌 경우 403을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @Parameter(description = "댓글 ID", example = "42") @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest req
    ) {
        Long id = requireCommentId(commentId);
        CommentUpdateRequest payload = Objects.requireNonNull(req, "request");
        return ResponseEntity.ok(ApiResponse.success(commentService.updateComment(id, payload)));
    }

    @DeleteMapping("/{commentId}")
    @Operation(
            summary = "댓글 삭제",
            description = "본인이 작성한 댓글을 삭제합니다. 본인이 아닌 경우 403을 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @Parameter(description = "댓글 ID", example = "42") @PathVariable Long commentId
    ) {
        commentService.deleteComment(requireCommentId(commentId));
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PutMapping("/{commentId}/like")
    @Operation(
            summary = "댓글 좋아요",
            description = """
                    댓글에 좋아요를 추가합니다.

                    **Response:**
                    - `liked`: true
                    - `likes`: 변경 후 좋아요 수
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> addLike(
            @Parameter(description = "댓글 ID", example = "42") @PathVariable Long commentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentService.addLike(requireCommentId(commentId))));
    }

    @DeleteMapping("/{commentId}/like")
    @Operation(
            summary = "댓글 좋아요 취소",
            description = """
                    댓글 좋아요를 취소합니다.

                    **Response:**
                    - `liked`: false
                    - `likes`: 변경 후 좋아요 수
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeLike(
            @Parameter(description = "댓글 ID", example = "42") @PathVariable Long commentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentService.removeLike(requireCommentId(commentId))));
    }

    @PutMapping("/{commentId}/pin")
    @Operation(
            summary = "댓글 고정",
            description = """
                    댓글을 고정합니다. 게시글 작성자만 사용 가능합니다. 게시글당 1개만 고정됩니다.

                    **Response:**
                    - `fixed`: true
                    - `commentId`: 고정된 댓글 ID
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> pinComment(
            @Parameter(description = "댓글 ID", example = "42") @PathVariable Long commentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentService.pinComment(requireCommentId(commentId))));
    }

    @DeleteMapping("/{commentId}/pin")
    @Operation(
            summary = "댓글 고정 해제",
            description = """
                    댓글 고정을 해제합니다. 게시글 작성자만 사용 가능합니다.

                    **Response:**
                    - `fixed`: false
                    - `commentId`: 해제된 댓글 ID
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> unpinComment(
            @Parameter(description = "댓글 ID", example = "42") @PathVariable Long commentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentService.unpinComment(requireCommentId(commentId))));
    }

    private @NonNull Long requireCommentId(Long commentId) {
        return Objects.requireNonNull(commentId, "commentId");
    }
}
