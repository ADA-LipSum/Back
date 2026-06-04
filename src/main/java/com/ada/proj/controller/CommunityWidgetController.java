package com.ada.proj.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.CommunityStatsResponse;
import com.ada.proj.dto.PopularTagResponse;
import com.ada.proj.service.CommunityStatsService;
import com.ada.proj.service.PopularTagService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/widget")
@Tag(name = "커뮤니티 위젯", description = "커뮤니티 우측 위젯 데이터 — 내 활동 통계, 인기 개발 태그 (최대 8개, 1시간 캐싱)")
public class CommunityWidgetController {

    private final CommunityStatsService communityStatsService;
    private final PopularTagService popularTagService;

    @GetMapping("/my-stats")
    @Operation(
            summary = "내 커뮤니티 통계",
            description = """
                    로그인 사용자의 커뮤니티 활동 통계를 반환합니다. **로그인 필요.**

                    **Response 필드**
                    | 필드 | 설명 |
                    |---|---|
                    | postCount | 작성한 커뮤니티 게시글 수 |
                    | receivedLikes | 내 게시글이 받은 총 좋아요 수 |
                    | commentCount | 작성한 댓글 수 |
                    | receivedReactions | 내 게시글이 받은 총 이모지 반응 수 |
                    | weeklyActivity | 이번 주 일별(일~토) 활동 수 배열 (게시글 + 댓글 합산) |
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "통계 반환 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요")
    })
    public ResponseEntity<ApiResponse<CommunityStatsResponse>> myStats(
            @Parameter(hidden = true) Authentication authentication
    ) {
        if (authentication == null) throw new SecurityException("로그인이 필요합니다.");
        return ResponseEntity.ok(ApiResponse.success(
                communityStatsService.getStats(authentication.getName())));
    }

    @GetMapping("/popular-tags")
    @Operation(
            summary = "인기 개발 태그 (상위 8개)",
            description = """
                    개발 커뮤니티(TECH 카테고리) 게시글의 기술 태그 사용 횟수 기준 **상위 8개**를 반환합니다.

                    - 결과는 Redis에 **1시간** 단위로 캐싱됩니다.
                    - 인증 불필요.

                    **Response 필드**
                    | 필드 | 설명 |
                    |---|---|
                    | tag | 태그 이름 (예: React) |
                    | count | 게시글 작성 시 태그 사용 횟수 |
                    """
    )
    public ResponseEntity<ApiResponse<List<PopularTagResponse>>> popularTags() {
        return ResponseEntity.ok(ApiResponse.success(popularTagService.getPopularTags()));
    }
}
