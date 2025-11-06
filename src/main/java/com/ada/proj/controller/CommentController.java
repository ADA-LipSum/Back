package com.ada.proj.controller;

import com.ada.proj.dto.*;
import com.ada.proj.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(@RequestBody CommentCreateRequest request) {
        return ResponseEntity.ok(commentService.createComment(request));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable String postId) {
        return ResponseEntity.ok(commentService.getCommentsByPost(postId));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public ResponseEntity<CommentResponse> updateComment(@PathVariable Long id,
                                                         @RequestBody CommentUpdateRequest request) {
        return ResponseEntity.ok(commentService.updateComment(id, request));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}