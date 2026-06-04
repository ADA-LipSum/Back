package com.ada.proj.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ada.proj.entity.AcademicSchedule;

@Repository
public interface AcademicScheduleRepository extends JpaRepository<AcademicSchedule, Long> {

    List<AcademicSchedule> findByEventDateBetweenOrderByEventDateAsc(LocalDate from, LocalDate to);
}
