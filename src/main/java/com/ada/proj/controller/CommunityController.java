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
import com.ada.proj.dto.CommentResponse;
import com.ada.proj.dto.GeneralCommunityCreateRequest;
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

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/posts")
@Tag(name = "일반 커뮤니티", description = "일반 커뮤니티(잡담·밈·프로젝트 자랑) 피드 — 게시글 CRUD, 미디어 타입 필터, 좋아요(5초 쿨다운), 북마크. 개발 커뮤니티는 '개발 커뮤니티' 태그 참고.")
public class CommunityController {

    private final PostService postService;
    private final CommentService commentService;
    private final ReactionBroadcastService broadcastService;

    @GetMapping
    @Operation(
            summary = "일반 커뮤니티 피드 조회",
            description = """
                    **일반 커뮤니티** 게시글 목록입니다. 개발 커뮤니티는 `GET /api/community/dev/posts` 를 사용하세요.

                    **category (글 종류)**
                    | 값 | 설명 |
                    |---|---|
                    | 생략 / ALL | 전체 (잡담 + 밈 + 프로젝트 자랑 포함) |
                    | CHAT | 잡담만 |
                    | MEME | 밈만 |
                    | PROJECT_SHOWCASE | 프로젝트 자랑만 |

                    > `category=TECH` 는 개발 커뮤니티 전용입니다. 여기서도 동작하지만 `/dev/posts` 사용을 권장합니다.

                    **mediaFilter (미디어 유형 필터)**
                    | 값 | 설명 |
                    |---|---|
                    | ALL (기본) | 전체 |
                    | PHOTO | 이미지 첨부 게시글만 |
                    | VIDEO | 영상 첨부 게시글만 |
                    | TEXT | 텍스트만 (이미지·영상 없는 글) |

                    **sort** `LATEST`(기본) / `POPULAR`(좋아요 내림차순)

                    응답의 `images`, `videos` 필드로 상세 조회 없이도 첨부 이미지·영상 URL 목록을 확인할 수 있습니다.
                    """
    )
    public ResponseEntity<ApiResponse<PageResponse<PostSummaryResponse>>> list(
            @Parameter(description = "페이지 번호 (0부터)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "글 종류 필터", schema = @Schema(allowableValues = {"ALL", "CHAT", "MEME", "PROJECT_SHOWCASE"}))
            @RequestParam(required = false) String category,
            @Parameter(description = "제목·본문 키워드 검색어", example = "점심")
            @RequestParam(required = false) String query,
            @Parameter(description = "정렬 방식", schema = @Schema(allowableValues = {"LATEST", "POPULAR"}), example = "LATEST")
            @RequestParam(required = false, defaultValue = "LATEST") String sort,
            @Parameter(description = "미디어 유형 필터", schema = @Schema(allowableValues = {"ALL", "PHOTO", "VIDEO", "TEXT"}), example = "ALL")
            @RequestParam(required = false, defaultValue = "ALL") String mediaFilter
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.search(
                PostBoardType.COMMUNITY,
                CommunityCategory.fromFilter(category),
                null,   // techSubTag — 일반 커뮤니티에서는 사용 안 함
                null,   // techTag    — 일반 커뮤니티에서는 사용 안 함
                query,
                page,
                size,
                SortType.from(sort),
                MediaFilter.from(mediaFilter)
        )));
    }

    @PostMapping
    @Operation(
            summary = "게시글 작성",
            description = """
                    일반 커뮤니티 게시글을 작성합니다. **로그인 필요.**

                    `communityCategory` 미전송 시 기본값은 `CHAT`(잡담)입니다.
                    개발 커뮤니티 게시글은 `POST /api/community/dev/posts`를 사용하세요.

                    **Response:** `data` = 생성된 게시글 순번(Long)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Long>> create(
            @Valid @RequestBody GeneralCommunityCreateRequest request,
            @Parameter(hidden = true) Authentication authentication
    ) {
        PostCreateRequest payload = Objects.requireNonNull(request).toPostCreateRequest();
        if (authentication != null) payload.setWriterUuid(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(postService.createCommunity(payload)));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "게시글 상세 조회",
            description = """
                    게시글 상세를 반환합니다. 호출 시 **조회수 +1** 됩니다.

                    **Response 주요 필드**
                    | 필드 | 설명 |
                    |---|---|
                    | emojiReactions | 이모지별 반응 수·내 반응 여부 목록 |
                    | isLiked | 로그인 사용자 좋아요 여부 (비로그인 false) |
                    | isBookmarked | 로그인 사용자 북마크 여부 |
                    | poll | 투표 게시글인 경우 투표 정보 |
                    | techTags | 기술 태그 목록 |
                    """
    )
    public ResponseEntity<ApiResponse<PostDetailResponse>> detail(
            @Parameter(description = "게시글 순번", example = "1") @PathVariable Long id,
            Authentication authentication
    ) {
        String requesterUuid = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(ApiResponse.success(postService.detail(id, requesterUuid)));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "게시글 수정",
            description = "작성자 본인 또는 관리자만 가능합니다. 전달한 필드만 부분 업데이트됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> update(
            @Parameter(description = "게시글 순번", example = "1") @PathVariable Long id,
            @Valid @RequestBody PostUpdateRequest request,
            @Parameter(hidden = true) Authentication authentication
    ) {
        postService.updateCommunity(id, request, authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "게시글 삭제",
            description = "작성자 본인 또는 관리자만 가능합니다. 연결된 투표·이모지 반응이 함께 삭제됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "게시글 순번", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication
    ) {
        postService.deleteCommunity(id, authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{id}/like")
    @Operation(
            summary = "좋아요 토글",
            description = """
                    좋아요를 토글합니다. **로그인 필요.**
                    - 좋아요 **추가** 시 **5초 쿨다운**이 적용됩니다 (5초 내 재요청 시 `400` 반환).
                    - 취소는 쿨다운 없이 즉시 가능합니다.
                    - 변경 후 WebSocket `/topic/post/{postUuid}/likes` 로 최대 **10초** 내 전파됩니다.

                    **Response:** `data` = `true`(추가됨) / `false`(취소됨)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Boolean>> toggleLike(
            @Parameter(description = "게시글 순번", example = "1") @PathVariable Long id,
            Authentication authentication
    ) {
        if (authentication == null) {
            throw new SecurityException("로그인이 필요합니다.");
        }
        String principal = Objects.requireNonNull(authentication.getName(), "principal");
        boolean liked = postService.toggleLike(principal, id);

        // WebSocket 브로드캐스트 예약
        String postUuid = postService.findUuidBySeq(id);
        broadcastService.markLikeChanged(postUuid);

        return ResponseEntity.ok(ApiResponse.success(liked));
    }

    @DeleteMapping("/likes/{likeId}")
    @Operation(
            summary = "좋아요 취소 (ID 기반)",
            description = "좋아요 고유 ID로 좋아요를 취소합니다. 본인의 좋아요만 취소 가능합니다. **로그인 필요.**",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> deleteLikeById(
            @Parameter(description = "좋아요 id", example = "1") @PathVariable Long likeId,
            Authentication authentication
    ) {
        if (authentication == null) {
            throw new SecurityException("로그인이 필요합니다.");
        }
        String principal = Objects.requireNonNull(authentication.getName(), "principal");
        postService.deleteLikeById(Objects.requireNonNull(likeId, "likeId"), principal);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{id}/bookmark")
    @Operation(
            summary = "북마크 토글",
            description = "북마크를 추가하거나 제거합니다. **로그인 필요.** `data` = `true`(추가) / `false`(제거)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Boolean>> toggleBookmark(
            @Parameter(description = "게시글 순번", example = "1") @PathVariable Long id,
            Authentication authentication
    ) {
        if (authentication == null) {
            throw new SecurityException("로그인이 필요합니다.");
        }
        String principal = Objects.requireNonNull(authentication.getName(), "principal");
        boolean bookmarked = postService.toggleBookmark(principal, id);
        return ResponseEntity.ok(ApiResponse.success(bookmarked));
    }

    @GetMapping("/bookmarks")
    @Operation(
            summary = "내 북마크 목록 조회",
            description = "로그인 사용자가 북마크한 커뮤니티 게시글을 최신순으로 반환합니다. **로그인 필요.**",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<PageResponse<PostSummaryResponse>>> myBookmarks(
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        if (authentication == null) {
            throw new SecurityException("로그인이 필요합니다.");
        }
        String principal = Objects.requireNonNull(authentication.getName(), "principal");
        return ResponseEntity.ok(ApiResponse.success(
                postService.getMyBookmarks(principal, PostBoardType.COMMUNITY, page, size)));
    }

    @GetMapping("/{id}/comments")
    @Operation(
            summary = "댓글 목록 조회",
            description = "게시글의 댓글·대댓글을 조회합니다. 인증 불필요. 댓글은 `children` 필드에 대댓글을 포함하는 재귀 구조입니다."
    )
    public ResponseEntity<ApiResponse<List<CommentResponse>>> comments(
            @Parameter(description = "게시글 순번", example = "1") @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(commentService.getCommentsByPost(id)));
    }

    private TechSubTag parseTechSubTag(String value) {
        if (value == null || value.isBlank() || "전체".equals(value.trim()) || "ALL".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return TechSubTag.from(value);
    }
}
