package com.ada.proj.controller;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.PageResponse;
import com.ada.proj.dto.PostCreateRequest;
import com.ada.proj.dto.PostDetailResponse;
import com.ada.proj.dto.PostSummaryResponse;
import com.ada.proj.enums.CommunityCategory;
import com.ada.proj.enums.MediaFilter;
import com.ada.proj.enums.PostBoardType;
import com.ada.proj.enums.SortType;
import com.ada.proj.enums.TechSubTag;
import com.ada.proj.service.PostService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 개발 커뮤니티 전용 컨트롤러.
 * 모든 조회는 category=TECH 로 고정됩니다.
 * 게시글 수정·삭제·좋아요·북마크는 /api/community/posts/{id} 엔드포인트를 사용하세요.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/dev/posts")
@Tag(name = "개발 커뮤니티",
        description = "개발 커뮤니티 전용 피드 — 질문·프로젝트·자료 공유, 언어 필터. "
                + "수정·삭제·좋아요·북마크는 `일반 커뮤니티` 태그의 `/api/community/posts/{id}` 사용.")
public class DevCommunityController {

    private final PostService postService;

    @GetMapping
    @Operation(
            summary = "개발 커뮤니티 피드 조회",
            description = """
                    **개발 커뮤니티** 게시글 목록입니다. 항상 `category=TECH` 로 고정됩니다.

                    **postType (게시물 유형)**
                    | 값 | 설명 |
                    |---|---|
                    | 생략 / ALL | 전체 (질문 + 프로젝트 + 자료 공유) |
                    | QUESTION | 질문 |
                    | PROJECT | 프로젝트 |
                    | RESOURCE_SHARING | 자료 공유 |

                    **language (언어 필터)**
                    - `React`, `Spring`, `Python`, `Java` 등 `techTags` 값으로 저장된 태그 이름을 그대로 입력
                    - 부분 일치 검색 (대소문자 무시)

                    **sort** `LATEST`(기본) / `POPULAR`(좋아요 내림차순)
                    """
    )
    public ResponseEntity<ApiResponse<PageResponse<PostSummaryResponse>>> list(
            @Parameter(description = "페이지 번호 (0부터)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "게시물 유형 필터",
                    schema = @Schema(allowableValues = {"ALL", "QUESTION", "PROJECT", "RESOURCE_SHARING"}))
            @RequestParam(required = false) String postType,
            @Parameter(description = "프로그래밍 언어 태그 필터 (예: React, Spring, Python)", example = "React")
            @RequestParam(required = false) String language,
            @Parameter(description = "제목·본문 키워드 검색어", example = "스프링 의존성")
            @RequestParam(required = false) String query,
            @Parameter(description = "정렬 방식", schema = @Schema(allowableValues = {"LATEST", "POPULAR"}), example = "LATEST")
            @RequestParam(required = false, defaultValue = "LATEST") String sort
    ) {
        TechSubTag subTag = parseTechSubTag(postType);
        return ResponseEntity.ok(ApiResponse.success(postService.search(
                PostBoardType.COMMUNITY,
                CommunityCategory.TECH,  // 개발 커뮤니티는 항상 TECH
                subTag,
                language,
                query,
                page,
                size,
                SortType.from(sort),
                MediaFilter.ALL
        )));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "개발 커뮤니티 게시글 상세 조회",
            description = "개발 커뮤니티 게시글 상세를 반환합니다. 호출 시 **조회수 +1**."
    )
    public ResponseEntity<ApiResponse<PostDetailResponse>> detail(
            @Parameter(description = "게시글 순번", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication
    ) {
        String requesterUuid = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(ApiResponse.success(postService.detail(id, requesterUuid)));
    }

    @PostMapping
    @Operation(
            summary = "개발 커뮤니티 게시글 작성",
            description = """
                    개발 커뮤니티 게시글을 작성합니다. **로그인 필요.** `communityCategory`는 자동으로 `TECH` 로 설정됩니다.

                    | 필드 | 필수 | 설명 |
                    |---|---|---|
                    | title | ✅ | 제목 (최대 20자) |
                    | content | - | 본문 (Markdown/HTML) |
                    | techSubTag | ✅ | `QUESTION`(질문) · `PROJECT`(프로젝트) · `RESOURCE_SHARING`(자료 공유) |
                    | techTags | - | 언어·기술 태그 (예: `["React","TypeScript"]`) |
                    | images | - | S3 이미지 URL 목록 (쉼표 구분) |

                    **Response:** `data` = 생성된 게시글 순번(Long)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Long>> create(
            @Valid @RequestBody PostCreateRequest request,
            @Parameter(hidden = true) Authentication authentication
    ) {
        PostCreateRequest payload = Objects.requireNonNull(request, "request");
        payload.setCommunityCategory(CommunityCategory.TECH);  // 강제 설정
        if (authentication != null) {
            payload.setWriterUuid(authentication.getName());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(postService.createCommunity(payload)));
    }

    private TechSubTag parseTechSubTag(String value) {
        if (value == null || value.isBlank()
                || "ALL".equalsIgnoreCase(value.trim())
                || "전체".equals(value.trim())) {
            return null;
        }
        return TechSubTag.from(value);
    }
}
