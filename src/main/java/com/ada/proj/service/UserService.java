package com.ada.proj.service;

import com.ada.proj.dto.*;
import com.ada.proj.entity.Role;
import com.ada.proj.entity.User;
import com.ada.proj.entity.UserData;
import com.ada.proj.repository.UserDataRepository;
import com.ada.proj.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserDataRepository userDataRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       UserDataRepository userDataRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userDataRepository = userDataRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> listUsers(Role role, String query) {
        return userRepository.search(role, query);
    }

    public UserProfileResponse getUserProfile(String uuid) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Optional<UserData> userDataOpt = userDataRepository.findByUuid(uuid);
        UserData ud = userDataOpt.orElse(null);

        return UserProfileResponse.builder()
                .uuid(user.getUuid())
                .adminId(user.getAdminId())
                .customId(user.getCustomId())
                .userRealname(user.getUserRealname())
                .userNickname(user.getUserNickname())
                .useNickname(user.isUseNickname())
                .profileImage(user.getProfileImage())
                .profileBanner(user.getProfileBanner())
                .role(user.getRole())
                .intro(ud == null ? null : ud.getIntro())
                .techStack(ud == null ? null : ud.getTechStack())
                .links(ud == null ? null : ud.getLinks())
                .badge(ud == null ? null : ud.getBadge())
                .activityScore(ud == null ? null : ud.getActivityScore())
                .contributionData(ud == null ? null : ud.getContributionData())
                .build();
    }

    public void updateRole(String uuid, Role role) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRole(role);
    }

    public void toggleUseNickname(String uuid) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setUseNickname(!user.isUseNickname());
    }

    public void updateProfile(String uuid, UpdateProfileRequest req) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (req.getNickname() != null) user.setUserNickname(req.getNickname());
        if (req.getProfileImage() != null) user.setProfileImage(req.getProfileImage());
        if (req.getProfileBanner() != null) user.setProfileBanner(req.getProfileBanner());
    }

    public void createCustomLogin(String uuid, CreateCustomLoginRequest req, Authentication auth) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        // 자신 또는 관리자만 허용
        ensureSelfOrAdmin(auth, uuid);
        if (user.getCustomId() != null) {
            throw new IllegalStateException("Custom ID already set");
        }
        if (userRepository.existsByCustomId(req.getCustomId())) {
            throw new IllegalArgumentException("Custom ID already exists");
        }
        user.setCustomId(req.getCustomId());
        user.setCustomPw(passwordEncoder.encode(req.getPassword()));
    }

    public void changeCustomPassword(String uuid, UpdatePasswordRequest req, Authentication auth) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        ensureSelfOrAdmin(auth, uuid);
        if (user.getCustomPw() == null || !passwordEncoder.matches(req.getCurrentPassword(), user.getCustomPw())) {
            throw new IllegalArgumentException("Current password does not match");
        }
        user.setCustomPw(passwordEncoder.encode(req.getNewPassword()));
    }

    private void ensureSelfOrAdmin(Authentication auth, String uuid) {
        if (auth == null) throw new SecurityException("Unauthenticated");
        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
        String principal = auth.getName();
        if (!isAdmin && !uuid.equals(principal)) {
            throw new SecurityException("Forbidden");
        }
    }
}
