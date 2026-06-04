package com.ada.proj.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CommunityStatsResponse", description = "내 커뮤니티 활동 통계 위젯 데이터")
public class CommunityStatsResponse {

    @Schema(description = "사용자 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String userUuid;

    @Schema(description = "실명", example = "홍길동")
    private String realName;

    @Schema(description = "닉네임", example = "gildong99")
    private String nickname;

    @Schema(description = "프로필 이미지 URL", example = "https://bucket.s3.ap-northeast-2.amazonaws.com/profiles/...")
    private String profileImage;

    @Schema(description = "작성한 커뮤니티 게시글 수", example = "17")
    private long postCount;

    @Schema(description = "내 게시글이 받은 총 좋아요 수", example = "43")
    private long receivedLikes;

    @Schema(description = "작성한 댓글 수", example = "28")
    private long commentCount;

    @Schema(description = "내 게시글이 받은 총 이모지 반응 수", example = "12")
    private long receivedReactions;

    @Schema(description = "이번 주(일~토) 일별 활동 수 배열 — 게시글+댓글 합산 (0=일요일, 6=토요일)",
            example = "[0,2,1,0,3,1,0]")
    private List<Integer> weeklyActivity;
}
