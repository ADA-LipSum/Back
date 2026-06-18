package com.ada.proj.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ada.proj.entity.StudyGroup;
import com.ada.proj.enums.StudyGroupCategory;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long>, JpaSpecificationExecutor<StudyGroup> {

    Optional<StudyGroup> findByGroupUuid(String groupUuid);

    org.springframework.data.domain.Page<StudyGroup> findByCategory(StudyGroupCategory category, org.springframework.data.domain.Pageable pageable);
}
