package com.ada.proj.repository;

import com.ada.proj.entity.GuestbookEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuestbookRepository extends JpaRepository<GuestbookEntry, Long> {

    List<GuestbookEntry> findByTargetUuidOrderByCreatedAtDesc(String targetUuid);

    boolean existsByTargetUuidAndWriterUuid(String targetUuid, String writerUuid);

    java.util.Optional<GuestbookEntry> findByTargetUuidAndWriterUuid(String targetUuid, String writerUuid);
}
