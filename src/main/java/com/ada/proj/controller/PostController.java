package com.ada.proj.controller;

import com.ada.proj.dto.*;
import com.ada.proj.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    // 생성
    @PostMapping
    @Operation(summary = "게시물 생성")
    public ResponseEntity<ApiResponse<String>> create(@Valid @RequestBody PostCreateRequest req) {
        String uuid = postService.create(req);
        return ResponseEntity.ok(ApiResponse.success(uuid));
    }

    // 목록
    @GetMapping
    @Operation(summary = "게시물 보기")
    public ResponseEntity<ApiResponse<Page<PostSummaryResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.list(page, size)));
    }

    // 상세 (조회수 +1)
    @Operation(summary = "조회수 증가")
    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> detail(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(postService.detail(uuid)));
    }

    // 수정
    @PatchMapping("/{uuid}")
    @Operation(summary = "게시물 수정")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable String uuid,
                                                    @RequestBody PostUpdateRequest req) {
        postService.update(uuid, req);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 삭제
    @DeleteMapping("/{uuid}")
    @Operation(summary = "게시물 삭제")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String uuid) {
        postService.delete(uuid);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 좋아요 +1
    @PostMapping("/{uuid}/like")
    @Operation(summary = "좋아요 수 증가")
    public ResponseEntity<ApiResponse<Void>> like(@PathVariable String uuid) {
        postService.like(uuid);
        return ResponseEntity.ok(ApiResponse.success());
    }
}