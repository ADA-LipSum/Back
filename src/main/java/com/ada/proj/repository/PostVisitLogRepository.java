package com.ada.proj.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ada.proj.entity.PostVisitLog;

public interface PostVisitLogRepository extends JpaRepository<PostVisitLog, Long> {

    boolean existsByUserUuidAndPostUuid(String userUuid, String postUuid);

    long countByUserUuid(String userUuid);
}
