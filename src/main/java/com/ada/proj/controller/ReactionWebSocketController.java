package com.ada.proj.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import com.ada.proj.dto.EmojiReactionRequest;
import com.ada.proj.dto.EmojiReactionResponse;
import com.ada.proj.service.EmojiReactionService;
import com.ada.proj.service.ReactionBroadcastService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * STOMP WebSocket 실시간 이모지 반응 핸들러.
 *
 * <h3>연결 방법 (클라이언트)</h3>
 * <pre>{@code
 * const socket = new SockJS('/ws');
 * const client = Stomp.over(socket);
 * client.connect({}, () => {
 *   // 1) 반응 목록 구독
 *   client.subscribe(`/topic/post/${postUuid}/reactions`, (msg) => {
 *     const reactions = JSON.parse(msg.body); // EmojiReactionResponse[]
 *   });
 *   // 2) 이모지 반응 전송 (로그인 토큰을 STOMP connect 헤더에 포함)
 *   client.send(`/app/post/${postUuid}/react`, {}, JSON.stringify({ emoji: '👍' }));
 * });
 * }</pre>
 *
 * <h3>배치 브로드캐스트 전용 토픽 (REST API 좋아요·댓글 변경 시 자동 전파)</h3>
 * <ul>
 *   <li>{@code /topic/post/{postUuid}/likes} — 좋아요 수 변경 알림 (10초 배치)</li>
 *   <li>{@code /topic/post/{postUuid}/comments} — 댓글 수 변경 알림 (10초 배치)</li>
 * </ul>
 *
 * <p>인증: STOMP connect 헤더에 {@code Authorization: Bearer <token>} 포함 권장.
 * 토큰 없이 연결해도 구독은 가능하나, 이모지 전송 시 인증 오류가 반환됩니다.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ReactionWebSocketController {

    private final EmojiReactionService emojiReactionService;
    private final ReactionBroadcastService broadcastService;

    /**
     * 이모지 반응 토글 (WebSocket 경로).
     *
     * <p>전송 경로: {@code /app/post/{postUuid}/react}</p>
     * <p>브로드캐스트: {@code /topic/post/{postUuid}/reactions} 으로 최신 반응 목록 즉시 전파</p>
     *
     * @param postUuid 게시글 UUID (postUuid, not seq)
     * @param request  이모지 반응 요청 ({@code { "emoji": "👍" }})
     * @param principal 인증 정보 (null이면 빈 목록 반환)
     * @return 변경 후 이모지 반응 전체 목록
     */
    @MessageMapping("/post/{postUuid}/react")
    @SendTo("/topic/post/{postUuid}/reactions")
    public List<EmojiReactionResponse> react(
            @DestinationVariable String postUuid,
            EmojiReactionRequest request,
            Principal principal
    ) {
        if (principal == null) {
            log.warn("Unauthenticated WebSocket reaction attempt on post {}", postUuid);
            return List.of();
        }
        try {
            emojiReactionService.toggleReaction(postUuid, principal.getName(), request.getEmoji());
        } catch (Exception e) {
            log.warn("Failed to toggle reaction via WebSocket: {}", e.getMessage());
        }
        return emojiReactionService.getReactions(postUuid, principal.getName());
    }
}
