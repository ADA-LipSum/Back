package com.ada.proj.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ada.proj.dto.EmojiReactionResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 이모지 반응, 좋아요, 댓글 수를 10초 배치로 WebSocket 브로드캐스트하는 서비스.
 * 변경된 게시글 UUID를 누적하다가 스케줄 타이머에 한 번에 broadcast.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReactionBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private final EmojiReactionService emojiReactionService;

    // 변경 발생한 postUuid 집합 (10초마다 flush)
    private final Set<String> pendingReactionUpdates = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingLikeUpdates = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingCommentCountUpdates = ConcurrentHashMap.newKeySet();

    public void markReactionChanged(String postUuid) {
        pendingReactionUpdates.add(postUuid);
    }

    public void markLikeChanged(String postUuid) {
        pendingLikeUpdates.add(postUuid);
    }

    public void markCommentCountChanged(String postUuid) {
        pendingCommentCountUpdates.add(postUuid);
    }

    @Scheduled(fixedDelay = 10_000) // 10초
    public void flushReactions() {
        if (pendingReactionUpdates.isEmpty()) return;

        Set<String> snapshot = Set.copyOf(pendingReactionUpdates);
        pendingReactionUpdates.removeAll(snapshot);

        for (String postUuid : snapshot) {
            try {
                List<EmojiReactionResponse> reactions = emojiReactionService.getReactions(postUuid, null);
                messagingTemplate.convertAndSend(
                        "/topic/post/" + postUuid + "/reactions",
                        Map.of("postUuid", postUuid, "reactions", reactions)
                );
            } catch (Exception e) {
                log.warn("Failed to broadcast reactions for post {}: {}", postUuid, e.getMessage());
            }
        }
    }

    @Scheduled(fixedDelay = 10_000)
    public void flushLikes() {
        if (pendingLikeUpdates.isEmpty()) return;

        Set<String> snapshot = Set.copyOf(pendingLikeUpdates);
        pendingLikeUpdates.removeAll(snapshot);

        for (String postUuid : snapshot) {
            try {
                messagingTemplate.convertAndSend(
                        "/topic/post/" + postUuid + "/likes",
                        Map.of("postUuid", postUuid, "type", "likes")
                );
            } catch (Exception e) {
                log.warn("Failed to broadcast likes for post {}: {}", postUuid, e.getMessage());
            }
        }
    }

    @Scheduled(fixedDelay = 10_000)
    public void flushCommentCounts() {
        if (pendingCommentCountUpdates.isEmpty()) return;

        Set<String> snapshot = Set.copyOf(pendingCommentCountUpdates);
        pendingCommentCountUpdates.removeAll(snapshot);

        for (String postUuid : snapshot) {
            try {
                messagingTemplate.convertAndSend(
                        "/topic/post/" + postUuid + "/comments",
                        Map.of("postUuid", postUuid, "type", "comments")
                );
            } catch (Exception e) {
                log.warn("Failed to broadcast comment count for post {}: {}", postUuid, e.getMessage());
            }
        }
    }
}
