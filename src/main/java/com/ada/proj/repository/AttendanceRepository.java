package com.ada.proj.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ada.proj.entity.Attendance;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    boolean existsByUserUuidAndAttendanceDate(String userUuid, LocalDate attendanceDate);

    long countByUserUuid(String userUuid);
}
