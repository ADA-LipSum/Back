package com.ada.proj.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ada.proj.entity.Poll;
import com.ada.proj.entity.Post;

public interface PollRepository extends JpaRepository<Poll, Long> {

    @EntityGraph(attributePaths = {"post", "options"})
    Optional<Poll> findByPost(Post post);

    @EntityGraph(attributePaths = {"post", "options"})
    Optional<Poll> findByPost_PostUuid(String postUuid);

    boolean existsByPost_PostUuid(String postUuid);

    @EntityGraph(attributePaths = {"post", "options"})
    List<Poll> findByEndsAtBeforeAndEndedNotifiedFalse(LocalDateTime now);
}
