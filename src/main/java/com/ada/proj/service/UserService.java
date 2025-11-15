package com.ada.proj.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ada.proj.dto.CreateCustomLoginRequest;
import com.ada.proj.dto.CreateUserRequest;
import com.ada.proj.dto.UpdatePasswordRequest;
import com.ada.proj.dto.UpdateProfileRequest;
import com.ada.proj.dto.UserProfileResponse;
import com.ada.proj.entity.Role;
import com.ada.proj.entity.User;
import com.ada.proj.entity.UserData;
import com.ada.proj.repository.UserDataRepository;
import com.ada.proj.repository.UserRepository;
import com.ada.proj.service.FileStorageService.StoredFile;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserDataRepository userDataRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    public UserService(UserRepository userRepository,
                       UserDataRepository userDataRepository,
                       PasswordEncoder passwordEncoder,
                       FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.userDataRepository = userDataRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
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

    /**
     * 프로필 이미지 업로드 + DB 저장 (서버 저장 → URL 반환 → DB 저장 일괄 처리)
     */
    public UserProfileResponse uploadProfileImage(String uuid, MultipartFile file, Authentication auth) throws IOException {
        ensureSelfOrAdmin(auth, uuid);
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        StoredFile saved = fileStorageService.storeImage(file);
        user.setProfileImage(saved.url());
        return getUserProfile(uuid);
    }

    /**
     * 프로필 배너 업로드 + DB 저장 (서버 저장 → URL 반환 → DB 저장 일괄 처리)
     */
    public UserProfileResponse uploadProfileBanner(String uuid, MultipartFile file, Authentication auth) throws IOException {
        ensureSelfOrAdmin(auth, uuid);
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        StoredFile saved = fileStorageService.storeImage(file);
        user.setProfileBanner(saved.url());
        return getUserProfile(uuid);
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
        user.setPassword(passwordEncoder.encode(req.getPassword()));
    }

    public User createUserByAdmin(CreateUserRequest req, Authentication auth) {
        ensureAdmin(auth);

        if (userRepository.findByAdminId(req.getAdminId()).isPresent()) {
            throw new IllegalArgumentException("adminId already exists");
        }

        if (req.getCustomId() != null && userRepository.existsByCustomId(req.getCustomId())) {
            throw new IllegalArgumentException("customId already exists");
        }

    User user = User.builder()
                .uuid(java.util.UUID.randomUUID().toString())
                .adminId(req.getAdminId())
                .customId(req.getCustomId())
        .password(req.getPassword() == null ? null : passwordEncoder.encode(req.getPassword()))
                .userRealname(req.getUserRealname())
                .userNickname(req.getUserNickname())
                .role(req.getRole() == null ? Role.STUDENT : req.getRole())
                .build();

        return userRepository.save(user);
    }

    /**
     * Create the very first ADMIN account when none exists. This can be called without authentication
     * but will fail if any ADMIN already exists.
     */
    public User createInitialAdmin(CreateUserRequest req) {
        // if any ADMIN exists, disallow unauthenticated init
        if (userRepository.existsByRole(Role.ADMIN)) {
            throw new SecurityException("Admin already exists");
        }

        if (userRepository.findByAdminId(req.getAdminId()).isPresent()) {
            throw new IllegalArgumentException("adminId already exists");
        }

    User user = User.builder()
                .uuid(java.util.UUID.randomUUID().toString())
                .adminId(req.getAdminId())
                .customId(req.getCustomId())
        .password(req.getPassword() == null ? null : passwordEncoder.encode(req.getPassword()))
                .userRealname(req.getUserRealname())
                .userNickname(req.getUserNickname())
                .role(Role.ADMIN)
                .build();

        return userRepository.save(user);
    }

    private void ensureAdmin(Authentication auth) {
        if (auth == null) throw new SecurityException("Unauthenticated");
        // 권한은 CustomUserDetailsService 에서 prefix 없이 ADMIN 으로 부여됨
        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ADMIN"));
        if (!isAdmin) throw new SecurityException("Forbidden");
    }

    public void changeCustomPassword(String uuid, UpdatePasswordRequest req, Authentication auth) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        ensureSelfOrAdmin(auth, uuid);
        if (user.getPassword() == null || !passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password does not match");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
    }

    private void ensureSelfOrAdmin(Authentication auth, String uuid) {
        if (auth == null) throw new SecurityException("Unauthenticated");
        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ADMIN"));
        String principal = auth.getName();
        if (!isAdmin && !uuid.equals(principal)) {
            throw new SecurityException("Forbidden");
        }
    }
}
