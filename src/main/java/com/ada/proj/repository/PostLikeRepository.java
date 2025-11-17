package com.ada.proj.repository;

import com.ada.proj.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByUserUuidAndPostUuid(String userUuid, String postUuid);

    boolean existsByUserUuidAndPostUuid(String userUuid, String postUuid);

    void deleteByUserUuidAndPostUuid(String userUuid, String postUuid);

    long countByPostUuid(String postUuid);
}