package com.ada.proj.controller;

import java.util.List;
import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ada.proj.dto.ApiResponse;
import com.ada.proj.dto.EmojiReactionRequest;
import com.ada.proj.dto.EmojiReactionResponse;
import com.ada.proj.service.EmojiReactionService;
import com.ada.proj.service.ReactionBroadcastService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/posts/{id}/reactions")
@Tag(name = "이모지 반응", description = "게시글 이모지 반응 조회·토글 — 디스코드 스타일. 변경 후 WebSocket `/topic/post/{postUuid}/reactions` 으로 최대 10초 내 전파됨.")
public class EmojiReactionController {

    private final EmojiReactionService emojiReactionService;
    private final ReactionBroadcastService broadcastService;

    @GetMapping
    @Operation(
            summary = "이모지 반응 목록 조회",
            description = """
                    게시글에 달린 이모지 반응을 반응 수 내림차순으로 반환합니다. 인증 불필요.

                    **Response 필드**
                    | 필드 | 설명 |
                    |---|---|
                    | emoji | 이모지 문자열 (예: 👍) |
                    | count | 해당 이모지의 반응 수 |
                    | reacted | 로그인 사용자가 해당 이모지로 반응했는지 여부 (비로그인 시 항상 false) |
                    """
    )
    public ResponseEntity<ApiResponse<List<EmojiReactionResponse>>> list(
            @Parameter(description = "게시글 순번", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication
    ) {
        String requesterUuid = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(ApiResponse.success(
                emojiReactionService.getReactionsBySeq(id, requesterUuid)));
    }

    @PostMapping
    @Operation(
            summary = "이모지 반응 토글",
            description = """
                    이모지 반응을 추가하거나 제거합니다. **로그인 필요.**

                    - 같은 이모지를 다시 전송하면 반응이 **취소**됩니다.
                    - 하나의 게시글에 여러 종류의 이모지를 동시에 반응할 수 있습니다.
                    - 변경 후 WebSocket `/topic/post/{postUuid}/reactions` 으로 최대 **10초** 내 전파됩니다.

                    **Response:** `data` = `true`(반응 추가됨) / `false`(반응 취소됨)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토글 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요",
                    content = @Content(examples = @ExampleObject(value = """
                            {"success":false,"errorCode":"UNAUTHORIZED","message":"인증이 필요합니다."}""")))
    })
    public ResponseEntity<ApiResponse<Boolean>> toggle(
            @Parameter(description = "게시글 순번", example = "1") @PathVariable Long id,
            @Valid @RequestBody EmojiReactionRequest request,
            @Parameter(hidden = true) Authentication authentication
    ) {
        if (authentication == null) throw new SecurityException("로그인이 필요합니다.");
        String userUuid = Objects.requireNonNull(authentication.getName());
        boolean added = emojiReactionService.toggleReactionBySeq(id, userUuid, request.getEmoji());

        String postUuid = emojiReactionService.findPostUuidBySeq(id);
        broadcastService.markReactionChanged(postUuid);

        return ResponseEntity.ok(ApiResponse.success(added));
    }
}
