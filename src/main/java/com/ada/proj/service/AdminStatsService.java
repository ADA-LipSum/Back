package com.ada.proj.service;

import com.ada.proj.dto.AdminStatsSummaryResponse;
import com.ada.proj.enums.Role;
import com.ada.proj.repository.CommentRepository;
import com.ada.proj.repository.NotificationRepository;
import com.ada.proj.repository.PostRepository;
import com.ada.proj.repository.StudyGroupRepository;
import com.ada.proj.repository.TradeLogRepository;
import com.ada.proj.repository.UserBanRepository;
import com.ada.proj.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserBanRepository userBanRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final TradeLogRepository tradeLogRepository;
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public AdminStatsSummaryResponse getSummary() {
        return AdminStatsSummaryResponse.builder()
                .totalUsers(userRepository.count())
                .totalStudents(userRepository.countByRole(Role.STUDENT))
                .totalTeachers(userRepository.countByRole(Role.TEACHER))
                .totalAdmins(userRepository.countByRole(Role.ADMIN))
                .totalPosts(postRepository.count())
                .totalComments(commentRepository.count())
                .activeBans(userBanRepository.countByActive(true))
                .totalGroups(studyGroupRepository.count())
                .totalTradeOrders(tradeLogRepository.count())
                .totalNotifications(notificationRepository.count())
                .build();
    }
}
