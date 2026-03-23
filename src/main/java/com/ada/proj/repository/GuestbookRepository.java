package com.ada.proj.repository;

import com.ada.proj.entity.GuestbookEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestbookRepository extends JpaRepository<GuestbookEntry, Long> {

    Page<GuestbookEntry> findByTargetUuidOrderByCreatedAtDesc(String targetUuid, Pageable pageable);

    boolean existsByTargetUuidAndWriterUuid(String targetUuid, String writerUuid);

    java.util.Optional<GuestbookEntry> findByTargetUuidAndWriterUuid(String targetUuid, String writerUuid);
}
