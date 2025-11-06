package com.ada.proj.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.PostCreateRequest;
import com.ada.proj.dto.PostDetailResponse;
import com.ada.proj.dto.PostUpdateRequest;
import com.ada.proj.service.PostService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/post")
@Tag(name = "게시물")
public class PostController {

    private final PostService postService;

    // 게시하기 (생성) - POST /post
    @PostMapping
    @Operation(
            summary = "게시물 생성",
            description = "새 게시물을 작성합니다. Swagger 우측 상단의 Authorize에서 Bearer 토큰을 입력해야 합니다. 작성자 UUID는 로그인 정보에서 자동 설정됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<String>> create(@Valid @RequestBody PostCreateRequest req,
                                                      Authentication authentication) {
        if (authentication != null) {
            // SecurityConfig에서 anyRequest().authenticated()이므로 null이 될 가능성은 낮지만 방어적으로 처리
            req.setWriterUuid(authentication.getName());
        }
        String uuid = postService.create(req);
        return ResponseEntity.ok(ApiResponse.success(uuid));
    }

    // 수정하기 - POST /post/update (게시글 ID 기준)
    @PostMapping("/update")
    @Operation(summary = "수정하기", description = "기존 게시글의 내용을 수정합니다 (게시글 ID 기준).", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> update(@RequestParam("uuid") String uuid,
                                                    @RequestBody PostUpdateRequest req,
                                                    Authentication authentication) {
        // 권한 검증이 필요하다면 authentication.getName()과 작성자 비교 로직을 Service로 전달
        postService.update(uuid, req);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 삭제하기 - POST /post/delete (게시글 ID 기준)
    @PostMapping("/delete")
    @Operation(summary = "삭제하기", description = "선택한 게시글을 삭제합니다 (게시글 ID 기준).", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> delete(@RequestParam("uuid") String uuid,
                                                    Authentication authentication) {
        postService.delete(uuid);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 자세히 보기 - GET /post/view (게시글 ID 기준, 조회수 +1)
    @GetMapping("/view")
    @Operation(summary = "자세히 보기", description = "게시글 상세 정보를 조회합니다 (게시글 ID 기준). 호출 시 조회수가 1 증가합니다.")
    public ResponseEntity<ApiResponse<PostDetailResponse>> detail(@RequestParam("uuid") String uuid) {
        return ResponseEntity.ok(ApiResponse.success(postService.detail(uuid)));
    }
}