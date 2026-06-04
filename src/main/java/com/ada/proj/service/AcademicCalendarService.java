package com.ada.proj.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.AcademicCalendarResponse;
import com.ada.proj.dto.AcademicScheduleEvent;
import com.ada.proj.dto.AcademicScheduleRequest;
import com.ada.proj.dto.AcademicScheduleResponse;
import com.ada.proj.entity.AcademicSchedule;
import com.ada.proj.repository.AcademicScheduleRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AcademicCalendarService {

    private final AcademicScheduleRepository scheduleRepository;

    // ── 공개 조회 ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AcademicCalendarResponse getMonthlyCalendar(int year, int month) {
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay  = firstDay.withDayOfMonth(firstDay.lengthOfMonth());

        List<AcademicScheduleEvent> events = scheduleRepository
                .findByEventDateBetweenOrderByEventDateAsc(firstDay, lastDay)
                .stream()
                .map(s -> AcademicScheduleEvent.builder()
                        .date(s.getEventDate().toString().replace("-", ""))
                        .eventName(s.getEventName())
                        .content(s.getContent())
                        .build())
                .toList();

        return AcademicCalendarResponse.builder()
                .year(year)
                .month(month)
                .events(events)
                .build();
    }

    // ── 관리자·교사 CRUD ───────────────────────────────────────────────────

    @Transactional
    public AcademicScheduleResponse create(AcademicScheduleRequest req, Authentication auth) {
        ensureAdminOrTeacher(auth);
        AcademicSchedule schedule = AcademicSchedule.builder()
                .eventDate(req.getEventDate())
                .eventName(req.getEventName().trim())
                .content(req.getContent() != null ? req.getContent().trim() : null)
                .createdBy(auth.getName())
                .build();
        return toResponse(scheduleRepository.save(schedule));
    }

    @Transactional
    public AcademicScheduleResponse update(Long id, AcademicScheduleRequest req, Authentication auth) {
        ensureAdminOrTeacher(auth);
        AcademicSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("학사 일정을 찾을 수 없습니다: " + id));
        schedule.setEventDate(req.getEventDate());
        schedule.setEventName(req.getEventName().trim());
        schedule.setContent(req.getContent() != null ? req.getContent().trim() : null);
        return toResponse(scheduleRepository.save(schedule));
    }

    @Transactional
    public void delete(Long id, Authentication auth) {
        ensureAdminOrTeacher(auth);
        if (!scheduleRepository.existsById(id)) {
            throw new EntityNotFoundException("학사 일정을 찾을 수 없습니다: " + id);
        }
        scheduleRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<AcademicScheduleResponse> listAll(int year) {
        LocalDate firstDay = LocalDate.of(year, 1, 1);
        LocalDate lastDay  = LocalDate.of(year, 12, 31);
        return scheduleRepository
                .findByEventDateBetweenOrderByEventDateAsc(firstDay, lastDay)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────────────────

    private void ensureAdminOrTeacher(Authentication auth) {
        if (auth == null) throw new AccessDeniedException("로그인이 필요합니다.");
        boolean allowed = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_TEACHER"));
        if (!allowed) throw new AccessDeniedException("관리자 또는 교사만 학사 일정을 관리할 수 있습니다.");
    }

    private AcademicScheduleResponse toResponse(AcademicSchedule s) {
        return AcademicScheduleResponse.builder()
                .id(s.getId())
                .eventDate(s.getEventDate())
                .eventName(s.getEventName())
                .content(s.getContent())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
