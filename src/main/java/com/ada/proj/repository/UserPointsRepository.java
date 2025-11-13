package com.ada.proj.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ada.proj.entity.UserPoints;

public interface UserPointsRepository extends JpaRepository<UserPoints, Long> {

    Optional<UserPoints> findByPointsUuid(String pointsUuid);

    Page<UserPoints> findByUserUuidOrderByCreatedAtDescSeqDesc(String userUuid, Pageable pageable);
}
