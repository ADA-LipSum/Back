package com.ada.proj.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.EmojiReactionResponse;
import com.ada.proj.entity.PostEmojiReaction;
import com.ada.proj.repository.PostEmojiReactionRepository;
import com.ada.proj.repository.PostRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmojiReactionService {

    private final PostEmojiReactionRepository reactionRepository;
    private final PostRepository postRepository;

    @Transactional(readOnly = true)
    public List<EmojiReactionResponse> getReactions(String postUuid, String requesterUuid) {
        List<Object[]> counts = reactionRepository.countByEmojiForPost(postUuid);
        List<PostEmojiReaction> userReactions = requesterUuid != null
                ? reactionRepository.findByPostUuid(postUuid).stream()
                        .filter(r -> requesterUuid.equals(r.getUserUuid()))
                        .toList()
                : List.of();

        Map<String, Boolean> reactedEmojis = userReactions.stream()
                .collect(Collectors.toMap(PostEmojiReaction::getEmoji, r -> true));

        return counts.stream()
                .map(row -> EmojiReactionResponse.builder()
                        .emoji((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .reacted(Boolean.TRUE.equals(reactedEmojis.get(row[0])))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmojiReactionResponse> getReactionsBySeq(Long seq, String requesterUuid) {
        String postUuid = findUuidBySeq(seq);
        return getReactions(postUuid, requesterUuid);
    }

    /**
     * 이모지 반응 토글 (추가/제거).
     * @return true = 반응 추가됨, false = 반응 제거됨
     */
    @Transactional
    public boolean toggleReaction(String postUuid, String userUuid, String emoji) {
        if (!postRepository.existsById(postUuid)) {
            throw new EntityNotFoundException("게시글을 찾을 수 없습니다: " + postUuid);
        }
        if (reactionRepository.existsByPostUuidAndUserUuidAndEmoji(postUuid, userUuid, emoji)) {
            reactionRepository.deleteByPostUuidAndUserUuidAndEmoji(postUuid, userUuid, emoji);
            return false;
        }
        reactionRepository.save(PostEmojiReaction.builder()
                .postUuid(postUuid)
                .userUuid(userUuid)
                .emoji(emoji)
                .build());
        return true;
    }

    @Transactional
    public boolean toggleReactionBySeq(Long seq, String userUuid, String emoji) {
        String postUuid = findUuidBySeq(seq);
        return toggleReaction(postUuid, userUuid, emoji);
    }

    @Transactional
    public void deleteAllByPost(String postUuid) {
        reactionRepository.deleteAllByPostUuid(postUuid);
    }

    public String findPostUuidBySeq(Long seq) {
        return postRepository.findBySeq(seq)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다: " + seq))
                .getPostUuid();
    }

    private String findUuidBySeq(Long seq) {
        return findPostUuidBySeq(seq);
    }
}
