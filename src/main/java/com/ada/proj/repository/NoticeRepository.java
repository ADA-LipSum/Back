package com.ada.proj.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ada.proj.entity.Notice;
import com.ada.proj.enums.NoticeTag;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, String> {

    Optional<Notice> findBySeq(Long seq);

    // 고정 게시물: pinnedOrder 오름차순
    List<Notice> findAllByIsPinnedTrueOrderByPinnedOrderAsc();

    // 일반 게시물 페이징 (tag 필터 포함)
    @Query("""
            select n from Notice n
            where n.isPinned = false
              and (:tag is null or n.tag = :tag)
            order by n.seq desc
            """)
    Page<Notice> findGeneralByTag(@Param("tag") NoticeTag tag, Pageable pageable);

    // tag 필터 없이 전체 일반 게시물
    @Query("""
            select n from Notice n
            where n.isPinned = false
            order by n.seq desc
            """)
    Page<Notice> findGeneralAll(Pageable pageable);

    // 고정 게시물 (tag 필터)
    @Query("""
            select n from Notice n
            where n.isPinned = true
              and (:tag is null or n.tag = :tag)
            order by n.pinnedOrder asc
            """)
    List<Notice> findPinnedByTag(@Param("tag") NoticeTag tag);

    @Modifying
    @Query("update Notice n set n.views = n.views + 1 where n.noticeUuid = :uuid")
    void incrementViews(@Param("uuid") String noticeUuid);

    @Query("select coalesce(max(n.pinnedOrder), 0) from Notice n where n.isPinned = true")
    int findMaxPinnedOrder();
}
