package com.ada.proj.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ada.proj.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientUuidOrderByCreatedAtDesc(String recipientUuid, Pageable pageable);

    Page<Notification> findBySenderUuidIsNotNullOrderByCreatedAtDesc(Pageable pageable);

    List<Notification> findByRecipientUuidAndReadAtIsNull(String recipientUuid);
}
