package com.ada.proj.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ada.proj.entity.CommunityEvent;

@Repository
public interface CommunityEventRepository extends JpaRepository<CommunityEvent, Long> {

    // 종료일이 오늘 이후인 이벤트만 반환 (시작일 오름차순)
    @Query("select e from CommunityEvent e where e.endDate >= :today order by e.startDate asc")
    List<CommunityEvent> findActiveEvents(@Param("today") LocalDate today);

    // 전체 목록 (관리자용, 시작일 오름차순)
    List<CommunityEvent> findAllByOrderByStartDateAsc();
}
