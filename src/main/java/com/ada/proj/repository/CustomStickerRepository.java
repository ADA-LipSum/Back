package com.ada.proj.repository;

import com.ada.proj.entity.CustomSticker;
import com.ada.proj.enums.CustomStickerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomStickerRepository extends JpaRepository<CustomSticker, Long> {

    Optional<CustomSticker> findByStickerUuid(String stickerUuid);

    Page<CustomSticker> findByUserUuidOrderByCreatedAtDesc(String userUuid, Pageable pageable);

    Page<CustomSticker> findByStatusOrderByCreatedAtAsc(CustomStickerStatus status, Pageable pageable);

    Page<CustomSticker> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<CustomSticker> findByUserUuidAndStatusOrderByCreatedAtDesc(String userUuid, CustomStickerStatus status, Pageable pageable);
}
