package com.ada.proj.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.PostCreateRequest;
import com.ada.proj.dto.PostDetailResponse;
import com.ada.proj.dto.PostSummaryResponse;
import com.ada.proj.dto.PostUpdateRequest;
import com.ada.proj.service.PostService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
@Tag(name = "게시물/게시")
public class PostController {

    private final PostService postService;

    // 생성
    @PostMapping
    @Operation(summary = "게시물 생성", description = "새 게시물을 작성합니다. 요청 바디로 `PostCreateRequest`를 전달하면 새 게시물의 UUID를 반환합니다. 인증이 필요한 경우 Authorization 헤더에 Bearer 토큰을 포함해야 합니다.")
    public ResponseEntity<ApiResponse<String>> create(@Valid @RequestBody PostCreateRequest req) {
        String uuid = postService.create(req);
        return ResponseEntity.ok(ApiResponse.success(uuid));
    }

    // 목록
    @GetMapping
    @Operation(summary = "게시물 목록 조회", description = "페이지와 사이즈 파라미터로 페이징된 게시물 요약 목록을 조회합니다. 기본값: page=0, size=10. 반환 형식은 `Page<PostSummaryResponse>`입니다.")
    public ResponseEntity<ApiResponse<Page<PostSummaryResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.list(page, size)));
    }

    // 상세 (조회수 +1)
    @Operation(summary = "게시물 상세 조회", description = "UUID로 게시물 상세 정보를 조회합니다. 호출 시 조회수가 1 증가합니다. 반환: `PostDetailResponse`.")
    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> detail(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponse.success(postService.detail(uuid)));
    }

    // 수정
    @PatchMapping("/{uuid}")
    @Operation(summary = "게시물 수정", description = "게시물의 일부 필드를 수정합니다. 요청 바디로 `PostUpdateRequest`를 전달합니다. 보통 작성자나 관리자만 수행할 수 있습니다.")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable String uuid,
                                                    @RequestBody PostUpdateRequest req) {
        postService.update(uuid, req);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 삭제
    @DeleteMapping("/{uuid}")
    @Operation(summary = "게시물 삭제", description = "게시물을 삭제합니다. 보통 작성자 또는 관리자 권한으로만 삭제할 수 있습니다. 삭제 성공 시 빈 응답을 반환합니다.")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String uuid) {
        postService.delete(uuid);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 좋아요 +1
    @PostMapping("/{uuid}/like")
    @Operation(summary = "좋아요 추가", description = "게시물에 좋아요를 추가합니다. 중복 좋아요 방지 로직이 서비스에 구현되어 있을 수 있습니다. 인증된 사용자만 호출해야 합니다.")
    public ResponseEntity<ApiResponse<Void>> like(@PathVariable String uuid) {
        postService.like(uuid);
        return ResponseEntity.ok(ApiResponse.success());
    }
}