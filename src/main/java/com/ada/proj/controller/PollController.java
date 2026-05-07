package com.ada.proj.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.PollResponse;
import com.ada.proj.dto.PollVoteRequest;
import com.ada.proj.service.PollService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/polls")
@Tag(name = "투표", description = "커뮤니티 투표 조회 및 투표 참여 API")
public class PollController {

    private final PollService pollService;

    @GetMapping("/posts/{postUuid}")
    @Operation(
            summary = "게시글 투표 상세 조회",
            description = """
                    커뮤니티 투표 게시글에 연결된 투표 정보를 조회합니다.

                    **Path Variable:**
                    - `postUuid` (필수): 투표가 연결된 게시글 UUID

                    **Response:**
                    - `id`: 투표 ID
                    - `postUuid`: 게시글 UUID
                    - `question`: 투표 질문
                    - `anonymous`: 익명 투표 여부
                    - `endsAt`: 종료 시각
                    - `ended`: 종료 여부
                    - `totalVotes`: 총 투표 수
                    - `myOptionId`: 로그인 사용자가 선택한 선택지 ID. 비로그인 또는 미참여 시 null
                    - `options`: 선택지 목록

                    익명 투표가 아니면 선택지별 `voters`에 투표자 정보가 포함됩니다.
                    """
    )
    public ResponseEntity<ApiResponse<PollResponse>> detail(
            @Parameter(description = "투표 게시글 UUID") @PathVariable String postUuid,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(pollService.getByPostUuid(postUuid, authentication)));
    }

    @PostMapping("/posts/{postUuid}/votes")
    @Operation(
            summary = "투표 참여 또는 선택지 변경",
            description = """
                    투표에 참여하거나 기존 선택지를 변경합니다. 로그인이 필요합니다.

                    **Path Variable:**
                    - `postUuid` (필수): 투표가 연결된 게시글 UUID

                    **Request Body:**
                    - `optionId` (필수): 선택할 투표 선택지 ID

                    **Rules:**
                    - 종료된 투표에는 참여할 수 없습니다.
                    - 한 사용자는 한 투표에 하나의 선택지만 선택할 수 있습니다.
                    - 이미 참여한 사용자가 다른 선택지를 보내면 기존 선택이 변경됩니다.

                    **Response:**
                    - 변경 후 투표 상세 정보
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<PollResponse>> vote(
            @Parameter(description = "투표 게시글 UUID") @PathVariable String postUuid,
            @Valid @RequestBody PollVoteRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(pollService.vote(postUuid, request, authentication)));
    }
}
