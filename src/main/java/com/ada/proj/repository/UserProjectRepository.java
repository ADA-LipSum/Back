package com.ada.proj.repository;

import com.ada.proj.entity.UserProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserProjectRepository extends JpaRepository<UserProject, Long> {

    List<UserProject> findByUserUuidOrderByCreatedAtDesc(String userUuid);

    Optional<UserProject> findByIdAndUserUuid(Long id, String userUuid);

    long countByUserUuid(String userUuid);
}
