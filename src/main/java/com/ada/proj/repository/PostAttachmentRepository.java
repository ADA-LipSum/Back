package com.ada.proj.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ada.proj.entity.PostAttachment;

@Repository
public interface PostAttachmentRepository extends JpaRepository<PostAttachment, Long> {

    List<PostAttachment> findByPostUuidOrderByDisplayOrderAsc(String postUuid);

    void deleteAllByPostUuid(String postUuid);

    long countByPostUuid(String postUuid);
}
