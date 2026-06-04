package com.ada.proj.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.CommunityStatsResponse;
import com.ada.proj.entity.User;
import com.ada.proj.repository.CommentRepository;
import com.ada.proj.repository.PostEmojiReactionRepository;
import com.ada.proj.repository.PostRepository;
import com.ada.proj.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommunityStatsService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostEmojiReactionRepository reactionRepository;

    @Transactional(readOnly = true)
    public CommunityStatsResponse getStats(String userUuid) {
        User user = userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        long postCount = postRepository.countCommunityPostsByWriter(userUuid);
        long receivedLikes = postRepository.sumLikesByWriter(userUuid);
        long commentCount = commentRepository.countByWriterUuid(userUuid);

        // 이모지 반응: 내 글에 달린 모든 반응 수
        long receivedReactions = countReceivedReactions(userUuid);

        // 이번 주 활동 그래프 (일~토, 7일)
        List<Integer> weeklyActivity = buildWeeklyActivity(userUuid);

        return CommunityStatsResponse.builder()
                .userUuid(userUuid)
                .realName(user.getUserRealname())
                .nickname(user.getUserNickname())
                .profileImage(user.getProfileImage())
                .postCount(postCount)
                .receivedLikes(receivedLikes)
                .commentCount(commentCount)
                .receivedReactions(receivedReactions)
                .weeklyActivity(weeklyActivity)
                .build();
    }

    private long countReceivedReactions(String userUuid) {
        try {
            return postRepository.findByWriterUuidOrderByWritedAtDesc(userUuid).stream()
                    .mapToLong(post -> reactionRepository.countByEmojiForPost(post.getPostUuid()).stream()
                            .mapToLong(row -> ((Number) row[1]).longValue())
                            .sum())
                    .sum();
        } catch (Exception e) {
            return 0L;
        }
    }

    private List<Integer> buildWeeklyActivity(String userUuid) {
        LocalDate today = LocalDate.now();
        LocalDate sunday = today.with(DayOfWeek.SUNDAY);
        if (today.getDayOfWeek() != DayOfWeek.SUNDAY) {
            sunday = today.minusDays(today.getDayOfWeek().getValue() % 7);
        }

        List<Integer> activity = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = sunday.plusDays(i);
            int posts = (int) postRepository.findByWriterUuidOrderByWritedAtDesc(userUuid).stream()
                    .filter(p -> p.getWritedAt() != null && p.getWritedAt().toLocalDate().equals(day))
                    .count();
            int comments = (int) commentRepository.countByWriterUuidAndDate(userUuid, day);
            activity.add(posts + comments);
        }
        return activity;
    }
}
