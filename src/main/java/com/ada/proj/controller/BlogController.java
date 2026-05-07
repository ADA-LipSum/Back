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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/blog/posts")
public class BlogController {

    private final PostService postService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PostSummaryResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String techTag,
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
    public ResponseEntity<ApiResponse<String>> create(
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

    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> detail(
            @PathVariable String uuid,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.detail(uuid, authentication)));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable String uuid,
            @Valid @RequestBody PostUpdateRequest request,
            Authentication authentication
    ) {
        postService.updateBlog(uuid, request, authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String uuid,
            Authentication authentication
    ) {
        postService.delete(uuid, authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
