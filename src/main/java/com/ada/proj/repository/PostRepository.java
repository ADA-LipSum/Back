// src/main/java/com/ada/proj/repository/PostRepository.java
package com.ada.proj.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ada.proj.entity.Post;

@Repository
public interface PostRepository extends JpaRepository<Post, String> {

    // 목록: 작성시간 최신순
    Page<Post> findAllByOrderByWritedAtDesc(Pageable pageable);

    // 조회수 +1
    @Modifying
    @Query("update Post p set p.views = p.views + 1 where p.postUuid = :uuid")
    int increaseViews(@Param("uuid") String uuid);

    // 좋아요 +1
    @Modifying
    @Query("update Post p set p.likes = p.likes + 1 where p.postUuid = :uuid")
    int increaseLikes(@Param("uuid") String uuid);

    // 좋아요 -1 (최소 0)
    @Modifying
    @Query("""
           update Post p
              set p.likes = case when p.likes > 0 then p.likes - 1 else 0 end
            where p.postUuid = :uuid
           """)
    int decreaseLikes(@Param("uuid") String uuid);

    // seq 기반 검색 지원
    java.util.Optional<Post> findBySeq(Long seq);
}