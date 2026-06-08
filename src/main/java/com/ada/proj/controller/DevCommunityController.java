package com.ada.proj.controller;

import java.util.List;
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
import com.ada.proj.dto.CommentResponse;
import com.ada.proj.dto.DevCommunityCreateRequest;
import com.ada.proj.dto.PageResponse;
import com.ada.proj.dto.PostCreateRequest;
import com.ada.proj.dto.PostDetailResponse;
import com.ada.proj.dto.PostSummaryResponse;
import com.ada.proj.dto.PostUpdateRequest;
import com.ada.proj.enums.CommunityCategory;
import com.ada.proj.enums.MediaFilter;
import com.ada.proj.enums.PostBoardType;
import com.ada.proj.enums.SortType;
import com.ada.proj.enums.TechSubTag;
import com.ada.proj.service.CommentService;
import com.ada.proj.service.PostService;
import com.ada.proj.service.ReactionBroadcastService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/dev/posts")
@Tag(name = "개발 커뮤니티", description = "개발 커뮤니티 전용 피드 — 질문·프로젝트·자료 공유, 언어 필터, CRUD 전부 포함")
public class DevCommunityController {

    private final PostService postService;
    private final CommentService commentService;
    private final ReactionBroadcastService broadcastService;

    // ── 조회 ────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(
            summary = "개발 커뮤니티 피드 조회",
            description = """
                    개발 커뮤니티 게시글 목록입니다. 항상 `category=TECH` 로 고정됩니다.

                    **postType (게시물 유형)**
                    | 값 | 설명 |
                    |---|---|
                    | 생략 / ALL | 전체 (질문 + 프로젝트 + 밈 + 자료 공유) |
                    | QUESTION | 질문 |
                    | PROJECT | 프로젝트 |
                    | MEME | 밈 |
                    | RESOURCE_SHARING | 자료 공유 |

                    **language** — `React`, `Spring`, `Python` 등 기술 태그 이름 (부분 일치, 대소문자 무시)

                    **sort** `LATEST`(기본) / `POPULAR`(좋아요 내림차순)

                    응답의 `images`, `videos` 필드로 상세 조회 없이도 첨부 이미지·영상 URL 목록을 확인할 수 있습니다.
                    """
    )
    public ResponseEntity<ApiResponse<PageResponse<PostSummaryResponse>>> list(
            @Parameter(description = "페이지 번호 (0부터)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "게시물 유형 필터",
                    schema = @Schema(allowableValues = {"ALL", "QUESTION", "PROJECT", "MEME", "RESOURCE_SHARING"}))
            @RequestParam(required = false) String postType,
            @Parameter(description = "프로그래밍 언어 태그 필터", example = "React")
            @RequestParam(required = false) String language,
            @Parameter(description = "제목·본문 키워드 검색어", example = "의존성 주입")
            @RequestParam(required = false) String query,
            @Parameter(description = "정렬 방식", schema = @Schema(allowableValues = {"LATEST", "POPULAR"}), example = "LATEST")
            @RequestParam(required = false, defaultValue = "LATEST") String sort
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.search(
                PostBoardType.COMMUNITY,
                CommunityCategory.TECH,
                parseTechSubTag(postType),
                language,
                query,
                page,
                size,
                SortType.from(sort),
                MediaFilter.ALL,
                null
        )));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "게시글 상세 조회",
            description = "개발 커뮤니티 게시글 상세를 반환합니다. 호출 시 **조회수 +1**."
    )
    public ResponseEntity<ApiResponse<PostDetailResponse>> detail(
            @Parameter(description = "게시글 순번", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication
    ) {
        String requesterUuid = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(ApiResponse.success(postService.detailDevCommunity(id, requesterUuid)));
    }

    @GetMapping("/{id}/comments")
    @Operation(
            summary = "댓글 목록 조회",
            description = "게시글의 댓글·대댓글을 조회합니다. 인증 불필요."
    )
    public ResponseEntity<ApiResponse<List<CommentResponse>>> comments(
            @Parameter(description = "게시글 순번", example = "1") @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getCommentsByPost(id)));
    }

    // ── 작성 / 수정 / 삭제 ────────────────────────────────────────────────

    @PostMapping
    @Operation(
            summary = "게시글 작성",
            description = """
                    개발 커뮤니티 게시글을 작성합니다. **로그인 필요.**
                    `communityCategory`는 자동으로 `TECH` 로 설정됩니다.

                    **Response:** `data` = 생성된 게시글 순번(Long)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Long>> create(
            @Valid @RequestBody DevCommunityCreateRequest request,
            @Parameter(hidden = true) Authentication authentication
    ) {
        PostCreateRequest payload = Objects.requireNonNull(request).toPostCreateRequest();
        if (authentication != null) payload.setWriterUuid(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(postService.createCommunity(payload)));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "게시글 수정",
            description = """
                    개발 커뮤니티 게시글을 수정합니다. **작성자 본인 또는 관리자만 가능.** 전달한 필드만 부분 업데이트됩니다.

                    | 필드 | 설명 |
                    |---|---|
                    | title | 제목 |
                    | content | 본문 |
                    | techSubTag | 게시물 유형 변경 (`QUESTION` · `PROJECT` · `RESOURCE_SHARING`) |
                    | techTags | 언어·기술 태그 변경 |
                    | images | 이미지 URL 변경 |
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> update(
            @Parameter(description = "게시글 순번", example = "1") @PathVariable Long id,
            @Valid @RequestBody PostUpdateRequest request,
            @Parameter(hidden = true) Authentication authentication
    ) {
        request.setCommunityCategory(CommunityCategory.TECH);
        postService.updateDevCommunity(id, request, authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "게시글 삭제",
            description = "작성자 본인 또는 관리자만 가능합니다. 연결된 이모지 반응이 함께 삭제됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "게시글 순번", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication
    ) {
        postService.deleteDevCommunity(id, authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // ── 좋아요 / 북마크 ───────────────────────────────────────────────────

    @PostMapping("/{id}/like")
    @Operation(
            summary = "좋아요 토글",
            description = """
                    좋아요를 추가하거나 취소합니다. **로그인 필요.**
                    - 좋아요 추가 시 **5초 쿨다운** 적용.
                    - 변경 후 WebSocket `/topic/post/{postUuid}/likes` 로 최대 10초 내 전파.

                    **Response:** `data` = `true`(추가) / `false`(취소)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Boolean>> toggleLike(
            @Parameter(description = "게시글 순번", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication
    ) {
        if (authentication == null) throw new SecurityException("로그인이 필요합니다.");
        boolean liked = postService.toggleLike(authentication.getName(), id);
        broadcastService.markLikeChanged(postService.findUuidBySeq(id));
        return ResponseEntity.ok(ApiResponse.success(liked));
    }

    @PostMapping("/{id}/bookmark")
    @Operation(
            summary = "북마크 토글",
            description = "북마크를 추가하거나 제거합니다. **로그인 필요.** `data` = `true`(추가) / `false`(제거)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Boolean>> toggleBookmark(
            @Parameter(description = "게시글 순번", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication
    ) {
        if (authentication == null) throw new SecurityException("로그인이 필요합니다.");
        boolean bookmarked = postService.toggleBookmark(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success(bookmarked));
    }

    // ── 내부 ─────────────────────────────────────────────────────────────

    private TechSubTag parseTechSubTag(String value) {
        if (value == null || value.isBlank()
                || "ALL".equalsIgnoreCase(value.trim())
                || "전체".equals(value.trim())) {
            return null;
        }
        return TechSubTag.from(value);
    }
}
