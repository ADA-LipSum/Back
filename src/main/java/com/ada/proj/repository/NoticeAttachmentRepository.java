package com.ada.proj.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ada.proj.entity.NoticeAttachment;

@Repository
public interface NoticeAttachmentRepository extends JpaRepository<NoticeAttachment, Long> {

    List<NoticeAttachment> findAllByNoticeUuidOrderByIdAsc(String noticeUuid);

    void deleteAllByNoticeUuid(String noticeUuid);
}
