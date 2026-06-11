package com.ada.proj.service;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.AttendanceResponse;
import com.ada.proj.entity.Attendance;
import com.ada.proj.enums.RewardActionCode;
import com.ada.proj.repository.AttendanceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AttendanceRepository attendanceRepository;
    private final RewardService rewardService;

    public AttendanceResponse checkIn(String userUuid) {
        LocalDate today = LocalDate.now(KST);

        if (attendanceRepository.existsByUserUuidAndAttendanceDate(userUuid, today)) {
            throw new IllegalStateException("오늘은 이미 출석체크를 완료했습니다.");
        }

        attendanceRepository.save(Attendance.builder()
                .userUuid(userUuid)
                .attendanceDate(today)
                .build());

        rewardService.grant(userUuid, RewardActionCode.ATTENDANCE_CHECK);

        return buildResponse(userUuid, today, true);
    }

    @Transactional(readOnly = true)
    public AttendanceResponse getStatus(String userUuid) {
        LocalDate today = LocalDate.now(KST);
        boolean checkedInToday = attendanceRepository.existsByUserUuidAndAttendanceDate(userUuid, today);
        return buildResponse(userUuid, today, checkedInToday);
    }

    private AttendanceResponse buildResponse(String userUuid, LocalDate date, boolean checkedInToday) {
        return AttendanceResponse.builder()
                .attendanceDate(date)
                .checkedInToday(checkedInToday)
                .totalCount(attendanceRepository.countByUserUuid(userUuid))
                .build();
    }
}
