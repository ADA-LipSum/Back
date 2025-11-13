package com.ada.proj.controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.CommentCreateRequest;
import com.ada.proj.dto.CommentResponse;
import com.ada.proj.dto.CommentUpdateRequest;
import com.ada.proj.service.CommentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Tag(name = "댓글")
public class CommentController {
    private final CommentService commentService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    @Operation(summary = "댓글 작성", description = "게시물에 댓글을 작성합니다. parentId가 있으면 대댓글로 등록됩니다.")
    public ResponseEntity<CommentResponse> createComment(@RequestBody CommentCreateRequest request) {
        return ResponseEntity.ok(commentService.createComment(request));
    }

    @GetMapping("/{postId}")
    @Operation(summary = "댓글 목록 조회", description = "특정 게시물(postUuid)의 최상위 댓글 목록과 각 댓글의 children을 조회합니다.")
    public ResponseEntity<List<CommentResponse>> getComments(
            @Parameter(description = "게시물 UUID (Post.postUuid)") @PathVariable String postId) {
        return ResponseEntity.ok(commentService.getCommentsByPost(postId));
    }

    @PutMapping("/comments/{commentId}")
    @Operation(summary = "댓글 수정", description = "로그인한 사용자가 자신의 댓글 내용을 수정합니다. 본인이 작성한 댓글만 수정할 수 있습니다.")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest req
    ) {
        return ResponseEntity.ok(commentService.updateComment(commentId, req));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "댓글 삭제", description = "로그인한 사용자가 자신의 댓글을 삭제합니다. 본인이 작성한 댓글만 삭제할 수 있습니다.")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/comments/{commentId}/like")
    @Operation(
            summary = "댓글 좋아요",
            description = "해당 댓글에 좋아요를 추가하거나 취소합니다. 토글 방식으로 동작합니다."
    )
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.toggleLike(commentId));
    }

    @PostMapping("/comments/{commentId}/fixed")
    @Operation(
            summary = "댓글 고정/해제",
            description = "해당 게시글의 작성자가 특정 댓글을 고정하거나 해제합니다. 댓글 작성자가 아니라, 게시글 작성자만 실행할 수 있습니다."
    )
    public ResponseEntity<Map<String, Object>> toggleFixed(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.toggleFixed(commentId));
    }
}
