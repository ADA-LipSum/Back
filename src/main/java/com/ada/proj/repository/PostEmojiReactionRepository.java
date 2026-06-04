package com.ada.proj.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ada.proj.entity.PostEmojiReaction;

@Repository
public interface PostEmojiReactionRepository extends JpaRepository<PostEmojiReaction, Long> {

    List<PostEmojiReaction> findByPostUuid(String postUuid);

    Optional<PostEmojiReaction> findByPostUuidAndUserUuidAndEmoji(String postUuid, String userUuid, String emoji);

    boolean existsByPostUuidAndUserUuidAndEmoji(String postUuid, String userUuid, String emoji);

    void deleteByPostUuidAndUserUuidAndEmoji(String postUuid, String userUuid, String emoji);

    void deleteAllByPostUuid(String postUuid);

    @Query("""
            select r.emoji, count(r) as cnt
            from PostEmojiReaction r
            where r.postUuid = :postUuid
            group by r.emoji
            order by cnt desc
            """)
    List<Object[]> countByEmojiForPost(@Param("postUuid") String postUuid);
}
