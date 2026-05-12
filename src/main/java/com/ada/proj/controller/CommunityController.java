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
import com.ada.proj.dto.PageResponse;
import com.ada.proj.dto.PostCreateRequest;
import com.ada.proj.dto.PostDetailResponse;
import com.ada.proj.dto.PostSummaryResponse;
import com.ada.proj.dto.PostUpdateRequest;
import com.ada.proj.enums.CommunityCategory;
import com.ada.proj.enums.PostBoardType;
import com.ada.proj.enums.TechSubTag;
import com.ada.proj.service.CommentService;
import com.ada.proj.service.PostService;

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
@Tag(name = "커뮤니티", description = "커뮤니티 게시글 CRUD, 좋아요, 댓글 조회 API")
public class CommunityController {

    private final PostService postService;
    private final CommentService commentService;

    @GetMapping
    @Operation(
            summary = "커뮤니티 게시글 목록 조회 및 검색",
            description = """
                    커뮤니티 게시글을 최신순으로 조회합니다. 상위 태그, 기술 하위 태그, 세부 기술 태그, 검색어를 조합해 필터링할 수 있습니다.

                    **상위 태그(category):**
                    - `ALL` 또는 `전체`: 전체 조회
                    - `CHAT`: 잡담
                    - `TECH`: 기술
                    - `MEME`: 밈
                    - `PROJECT_SHOWCASE`: 프로젝트 자랑

                    **기술 하위 태그(techSubTag):**
                    - `QUESTION`: 질문
                    - `CHAT`: 잡담
                    - `TIP`: 팁
                    - `POLL`: 투표

                    **Query Parameters:**
                    - `page` (선택): 페이지 번호, 0부터 시작
                    - `size` (선택): 페이지 크기
                    - `category` (선택): 상위 태그 필터
                    - `techSubTag` (선택): 기술 하위 태그 필터
                    - `techTag` (선택): React, MySQL 같은 세부 기술 태그
                    - `query` (선택): 제목/본문 검색어

                    **Response:**
                    - `data.content`: 게시글 요약 목록
                    - `boardType`: 항상 `COMMUNITY`
                    - `communityCategory`: 커뮤니티 상위 태그
                    - `techSubTag`: 기술 게시글의 하위 태그
                    - `techTags`: 세부 기술 태그 목록
                    """
    )
    public ResponseEntity<ApiResponse<PageResponse<PostSummaryResponse>>> list(
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "상위 태그 필터", schema = @Schema(allowableValues = {"ALL", "CHAT", "TECH", "MEME", "PROJECT_SHOWCASE"}))
            @RequestParam(required = false) String category,
            @Parameter(description = "기술 하위 태그 필터", schema = @Schema(allowableValues = {"QUESTION", "CHAT", "TIP", "POLL"}))
            @RequestParam(required = false) String techSubTag,
            @Parameter(description = "세부 기술 태그 필터", example = "React")
            @RequestParam(required = false) String techTag,
            @Parameter(description = "제목/본문 검색어", example = "상태 관리")
            @RequestParam(required = false) String query
    ) {
        return ResponseEntity.ok(ApiResponse.success(postService.search(
                PostBoardType.COMMUNITY,
                CommunityCategory.fromFilter(category),
                parseTechSubTag(techSubTag),
                techTag,
                query,
                page,
                size
        )));
    }

    @PostMapping
    @Operation(
            summary = "커뮤니티 게시글 작성",
            description = """
                    커뮤니티 게시글을 작성합니다. 로그인이 필요합니다.

                    **Request Body:**
                    - `title` (필수): 제목, 최대 20자
                    - `content` (선택): 본문
                    - `communityCategory` (필수 권장): `CHAT`, `TECH`, `MEME`, `PROJECT_SHOWCASE`
                    - `techSubTag` (조건부 필수): `communityCategory`가 `TECH`일 때 반드시 1개 선택
                    - `techTags` (선택): React, MySQL 등 세부 기술 태그 목록
                    - `poll` (조건부): `techSubTag`가 `POLL`일 때 투표 생성 정보

                    **기술 게시글 규칙:**
                    - `communityCategory=TECH`이면 `techSubTag`를 반드시 전달해야 합니다.
                    - `techSubTag=POLL`이면 투표 선택지와 종료 시각을 포함한 `poll` 정보를 함께 전달합니다.

                    **Response:**
                    - `data`: 생성된 게시글 순번 (Long)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Long>> create(
            @Valid @RequestBody PostCreateRequest request,
            Authentication authentication
    ) {
        PostCreateRequest payload = Objects.requireNonNull(request, "request");
        if (authentication != null) {
            payload.setWriterUuid(authentication.getName());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(postService.createCommunity(payload)));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "커뮤니티 게시글 상세 조회",
            description = """
                    커뮤니티 게시글 상세 정보를 조회합니다. 조회 시 조회수가 1 증가합니다.

                    **Path Variable:**
                    - `id` (필수): 게시글 순번

                    **Response:**
                    - 제목, 본문, 작성자 정보
                    - 좋아요 수, 조회 수, 댓글 수
                    - `isLiked`: 현재 로그인 사용자의 좋아요 여부 (비로그인 시 false)
                    - 커뮤니티 상위 태그, 기술 하위 태그, 세부 기술 태그
                    - 투표 게시글인 경우 `poll` 정보
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
            summary = "커뮤니티 게시글 수정",
            description = """
                    커뮤니티 게시글을 수정합니다. 작성자 본인 또는 관리자만 가능합니다.

                    **Path Variable:**
                    - `id` (필수): 수정할 게시글 순번

                    **Request Body:**
                    - `title`: 제목
                    - `content`: 본문
                    - `communityCategory`: 상위 태그
                    - `techSubTag`: 기술 하위 태그
                    - `techTags`: 세부 기술 태그 목록
                    - `poll`: 투표 정보. 투표 게시글에서만 사용 가능

                    **Response:** 성공 응답
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> update(
            @Parameter(description = "게시글 순번", example = "1") @PathVariable Long id,
            @Valid @RequestBody PostUpdateRequest request,
            Authentication authentication
    ) {
        postService.updateCommunity(id, request, authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "커뮤니티 게시글 삭제",
            description = """
                    커뮤니티 게시글을 삭제합니다. 작성자 본인 또는 관리자만 가능합니다.

                    **Path Variable:**
                    - `id` (필수): 삭제할 게시글 순번

                    연결된 투표가 있다면 함께 삭제됩니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "게시글 순번", example = "1") @PathVariable Long id,
            Authentication authentication
    ) {
        postService.delete(id, authentication);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{id}/like")
    @Operation(
            summary = "게시글 좋아요 토글",
            description = """
                    게시글 좋아요를 토글합니다. 로그인이 필요합니다.

                    **Path Variable:**
                    - `id` (필수): 대상 게시글 순번

                    **Response:**
                    - `data`: 변경 후 좋아요 상태 (true: 좋아요 추가됨, false: 좋아요 취소됨)
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
        return ResponseEntity.ok(ApiResponse.success(liked));
    }

    @DeleteMapping("/likes/{likeId}")
    @Operation(
            summary = "좋아요 취소 (ID 기반)",
            description = """
                    좋아요 고유 ID로 좋아요를 취소합니다. 로그인이 필요합니다. 본인이 누른 좋아요만 취소 가능합니다.

                    **Path Variable:**
                    - `likeId` (필수): 취소할 좋아요의 숫자 ID

                    **Response:** 성공 응답 (data: null)
                    """,
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

    @GetMapping("/{id}/comments")
    @Operation(
            summary = "게시글 댓글 조회",
            description = """
                    게시글에 달린 댓글 및 대댓글 전체를 조회합니다. 인증 불필요.

                    댓글은 `children` 필드로 대댓글을 포함하는 재귀 구조로 반환됩니다.
                    """
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
