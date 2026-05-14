package com.ada.proj.repository;

import com.ada.proj.entity.PostBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostBookmarkRepository extends JpaRepository<PostBookmark, Long> {
    Optional<PostBookmark> findByUserUuidAndPostUuid(String userUuid, String postUuid);
    boolean existsByUserUuidAndPostUuid(String userUuid, String postUuid);
    void deleteByUserUuidAndPostUuid(String userUuid, String postUuid);
}
