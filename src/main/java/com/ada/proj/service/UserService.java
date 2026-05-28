package com.ada.proj.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

import com.ada.proj.dto.UserBalanceSummary;
import com.ada.proj.repository.UserCoinsBalanceRepository;
import com.ada.proj.repository.UserPointsBalanceRepository;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ada.proj.dto.ActivitySummary;
import com.ada.proj.dto.CreateCustomLoginRequest;
import com.ada.proj.dto.CreateUserRequest;
import com.ada.proj.dto.SocialLinks;
import com.ada.proj.dto.UpdatePasswordRequest;
import com.ada.proj.dto.UpdateProfileRequest;
import com.ada.proj.dto.UserProfileResponse;
import com.ada.proj.enums.Role;
import com.ada.proj.entity.User;
import com.ada.proj.entity.UserData;
import com.ada.proj.exception.ForbiddenException;
import com.ada.proj.exception.UnauthenticatedException;
import com.ada.proj.exception.UserNotFoundException;
import com.ada.proj.repository.CommentRepository;
import com.ada.proj.repository.PostRepository;
import com.ada.proj.repository.UserDataRepository;
import com.ada.proj.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserDataRepository userDataRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;
    private final UserCoinsBalanceRepository coinsBalanceRepository;
    private final UserPointsBalanceRepository pointsBalanceRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public UserService(UserRepository userRepository,
            UserDataRepository userDataRepository,
            PostRepository postRepository,
            CommentRepository commentRepository,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper,
            CacheManager cacheManager,
            UserCoinsBalanceRepository coinsBalanceRepository,
            UserPointsBalanceRepository pointsBalanceRepository) {
        this.userRepository = userRepository;
        this.userDataRepository = userDataRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.cacheManager = cacheManager;
        this.coinsBalanceRepository = coinsBalanceRepository;
        this.pointsBalanceRepository = pointsBalanceRepository;
    }

    public List<User> listUsers(Role role, String query) {
        return userRepository.search(role, query);
    }

    public UserProfileResponse getUserProfile(String uuid) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return buildUserProfileResponse(user);
    }

    public UserProfileResponse getUserProfileByUsername(String username) {
        User user = userRepository.findByCustomId(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return buildUserProfileResponse(user);
    }

    private UserProfileResponse buildUserProfileResponse(User user) {
        String uuid = user.getUuid();
        Optional<UserData> userDataOpt = userDataRepository.findByUuid(uuid);
        UserData ud = userDataOpt.orElse(null);

        List<String> techList = null;
        if (ud != null && ud.getTechStack() != null) {
            techList = parseTechStack(ud.getTechStack());
        }

        SocialLinks socialLinks = ud == null ? null : SocialLinks.builder()
                .githubUrl(ud.getGithubUrl())
                .notionUrl(ud.getNotionUrl())
                .linkedinUrl(ud.getLinkedinUrl())
                .personalWebsiteUrl(ud.getPersonalWebsiteUrl())
                .build();

        ActivitySummary activitySummary = ActivitySummary.builder()
                .postCount(postRepository.countByWriterUuid(uuid))
                .commentCount(commentRepository.countByAuthor_Uuid(uuid))
                .build();

        return UserProfileResponse.builder()
                .uuid(uuid)
                .adminId(user.getAdminId())
                .customId(user.getCustomId())
                .userRealname(user.getUserRealname())
                .userNickname(user.getUserNickname())
                .useNickname(user.isUseNickname())
                .profileImage(user.getProfileImage())
                .profileBanner(user.getProfileBanner())
                .role(user.getRole())
                .githubAccount(user.getGithubLogin())
                .intro(ud == null ? null : ud.getIntro())
                .techStack(techList)
                .badge(ud == null ? null : ud.getBadge())
                .activityScore(ud == null ? null : ud.getActivityScore())
                .contributionData(ud == null ? null : ud.getContributionData())
                .socialLinks(socialLinks)
                .activitySummary(activitySummary)
                .build();
    }

    public void updateRole(String uuid, Role role) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setRole(role);
        userRepository.save(user);
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "users", key = "'profile:' + #uuid"),
        @CacheEvict(cacheNames = "users", key = "#uuid")
    })
    public void toggleUseNickname(String uuid) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setUseNickname(!user.isUseNickname());
        userRepository.save(user);
        evictUsernameCaches(user.getCustomId());
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "users", key = "'profile:' + #uuid"),
        @CacheEvict(cacheNames = "users", key = "#uuid")
    })
    public void updateProfile(String uuid, UpdateProfileRequest req) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        if (req.getNickname() != null) {
            user.setUserNickname(req.getNickname());
        }

        // upsert user_data
        UserData ud = userDataRepository.findByUuid(uuid).orElseGet(() ->
                UserData.builder().uuid(uuid).build());

        if (req.getIntro() != null) {
            ud.setIntro(req.getIntro());
        }
        if (req.getTechStack() != null) {
            ud.setTechStack(serializeTechStack(req.getTechStack()));
        }
        if (req.getGithubUrl() != null) {
            ud.setGithubUrl(req.getGithubUrl());
        }
        if (req.getNotionUrl() != null) {
            ud.setNotionUrl(req.getNotionUrl());
        }
        if (req.getLinkedinUrl() != null) {
            ud.setLinkedinUrl(req.getLinkedinUrl());
        }
        if (req.getPersonalWebsiteUrl() != null) {
            ud.setPersonalWebsiteUrl(req.getPersonalWebsiteUrl());
        }

        userDataRepository.save(ud);

        // 캐시에서 꺼낸 엔티티는 detached 상태일 수 있으므로 명시적으로 저장
        userRepository.save(user);

        // by-username 조회 캐시도 무효화 (annotation evict는 uuid 기반만 처리)
        evictUsernameCaches(user.getCustomId());
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "users", key = "'profile:' + #uuid"),
        @CacheEvict(cacheNames = "users", key = "#uuid")
    })
    public void updateProfileImage(String uuid, String url) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setProfileImage(url);
        userRepository.save(user);
        evictUsernameCaches(user.getCustomId());
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "users", key = "'profile:' + #uuid"),
        @CacheEvict(cacheNames = "users", key = "#uuid")
    })
    public void updateProfileBanner(String uuid, String url) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setProfileBanner(url);
        userRepository.save(user);
        evictUsernameCaches(user.getCustomId());
    }

    private void evictUsernameCaches(String customId) {
        if (customId == null) return;
        var cache = cacheManager.getCache("users");
        if (cache != null) {
            cache.evict("profile:username:" + customId);
            cache.evict("custom:" + customId);
        }
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "users", key = "#uuid"),
        @CacheEvict(cacheNames = "users", key = "'custom:' + #req.customId")
    })
    public void createCustomLogin(String uuid, CreateCustomLoginRequest req, Authentication auth) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        // 자신 또는 관리자만 허용
        ensureSelfOrAdmin(auth, uuid);
        if (user.isUseCustomId()) {
            throw new IllegalStateException("Custom ID already set");
        }
        if (userRepository.existsByCustomId(req.getCustomId())) {
            throw new IllegalArgumentException("Custom ID already exists");
        }
        user.setCustomId(req.getCustomId());
        user.setUseCustomId(!req.getCustomId().equals(user.getAdminId()));
        user.setPassword(passwordEncoder.encode(req.getPassword()));
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "users", key = "#result.uuid", condition = "#result != null"),
        @CacheEvict(cacheNames = "users", key = "'admin:' + #req.adminId"),
        @CacheEvict(cacheNames = "users", key = "'custom:' + #req.customId", condition = "#req.customId != null")
    })
    public User createUserByAdmin(CreateUserRequest req, Authentication auth) {
        ensureAdmin(auth);

        if (req.getUserNickname() == null || req.getUserNickname().isBlank()) {
            throw new IllegalArgumentException("userNickname is required");
        }

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
                .useCustomId(req.getCustomId() != null && !req.getCustomId().equals(req.getAdminId()))
                .password(req.getPassword() == null ? null : passwordEncoder.encode(req.getPassword()))
                .userRealname(req.getUserRealname())
                .userNickname(req.getUserNickname())
                .role(req.getRole() == null ? Role.STUDENT : req.getRole())
                .profileImage(req.getProfileImage())
                .profileBanner(req.getProfileBanner())
                .build();

        applyDefaultIdenticonProfileImageIfMissing(user);
        user = userRepository.save(Objects.requireNonNull(user));

        if (req.getIntro() != null || req.getTechStack() != null) {
            UserData ud = new UserData();
            ud.setUuid(user.getUuid());
            if (req.getIntro() != null) {
                ud.setIntro(req.getIntro());
            }
            if (req.getTechStack() != null) {
                ud.setTechStack(serializeTechStack(req.getTechStack()));
            }
            userDataRepository.save(ud);
        }

        return user;
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "users", key = "#result.uuid", condition = "#result != null"),
        @CacheEvict(cacheNames = "users", key = "'admin:' + #req.adminId"),
        @CacheEvict(cacheNames = "users", key = "'custom:' + #req.customId", condition = "#req.customId != null")
    })
    public User createInitialAdmin(CreateUserRequest req) {
        // if any ADMIN exists, disallow unauthenticated init
        if (userRepository.existsByRole(Role.ADMIN)) {
            throw new ForbiddenException("Admin already exists");
        }

        if (req.getUserNickname() == null || req.getUserNickname().isBlank()) {
            throw new IllegalArgumentException("userNickname is required");
        }

        if (userRepository.findByAdminId(req.getAdminId()).isPresent()) {
            throw new IllegalArgumentException("adminId already exists");
        }

        User user = User.builder()
                .uuid(java.util.UUID.randomUUID().toString())
                .adminId(req.getAdminId())
                .customId(req.getCustomId())
                .useCustomId(req.getCustomId() != null && !req.getCustomId().equals(req.getAdminId()))
                .password(req.getPassword() == null ? null : passwordEncoder.encode(req.getPassword()))
                .userRealname(req.getUserRealname())
                .userNickname(req.getUserNickname())
                .role(Role.ADMIN)
                .profileImage(req.getProfileImage())
                .profileBanner(req.getProfileBanner())
                .build();

        forceApplyIdenticonProfileImage(user);
        user = userRepository.save(Objects.requireNonNull(user));

        if (req.getIntro() != null || req.getTechStack() != null) {
            UserData ud = new UserData();
            ud.setUuid(user.getUuid());
            if (req.getIntro() != null) {
                ud.setIntro(req.getIntro());
            }
            if (req.getTechStack() != null) {
                ud.setTechStack(serializeTechStack(req.getTechStack()));
            }
            userDataRepository.save(ud);
        }

        return user;
    }

    private void applyDefaultIdenticonProfileImageIfMissing(User user) {
        applyIdenticonProfileImage(user, false);
    }

    private void forceApplyIdenticonProfileImage(User user) {
        applyIdenticonProfileImage(user, true);
    }

    private void applyIdenticonProfileImage(User user, boolean forceApply) {
        if (user == null) {
            return;
        }
        String profileImage = user.getProfileImage();
        if (!forceApply && profileImage != null && !profileImage.isBlank()) {
            return;
        }
        String uuid = user.getUuid();
        String seed = uuid == null
                ? java.util.UUID.randomUUID().toString()
                : uuid.replace("-", "");
        String encodedSeed = URLEncoder.encode(seed, StandardCharsets.UTF_8);
        String url = "https://api.dicebear.com/9.x/identicon/svg?seed=" + encodedSeed;
        user.setProfileImage(url);
    }

    private void ensureAdmin(Authentication auth) {
        if (auth == null) {
            throw new UnauthenticatedException("Unauthenticated");
        }
        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new ForbiddenException("Forbidden");
        }
    }

    private void ensureAdminOrTeacher(Authentication auth) {
        if (auth == null) {
            throw new UnauthenticatedException("Unauthenticated");
        }
        boolean ok = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_TEACHER"));
        if (!ok) {
            throw new ForbiddenException("Forbidden");
        }
    }

    @Caching(evict = {
        @CacheEvict(cacheNames = "users", key = "'profile:' + #uuid"),
        @CacheEvict(cacheNames = "users", key = "#uuid")
    })
    public void changeCustomPassword(String uuid, UpdatePasswordRequest req, Authentication auth) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        ensureSelfOrAdmin(auth, uuid);
        if (user.getPassword() == null || !passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password does not match");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    private void ensureSelfOrAdmin(Authentication auth, String uuid) {
        if (auth == null) {
            throw new UnauthenticatedException("Unauthenticated");
        }
        boolean isAdmin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals("ROLE_ADMIN"));
        String principal = auth.getName();
        if (!isAdmin && !uuid.equals(principal)) {
            throw new ForbiddenException("Forbidden");
        }
    }

    private List<String> parseTechStack(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        try {
            String s = stored.trim();
            if (s.startsWith("[")) {
                // JSON array
                return objectMapper.readValue(s, new TypeReference<List<String>>() {
                });
            }
            // CSV fallback
            List<String> list = new ArrayList<>();
            for (String it : s.split(",")) {
                String v = it.trim();
                if (!v.isEmpty()) {
                    list.add(v);
                }
            }
            return list.isEmpty() ? null : list;
        } catch (IOException e) {
            return null;
        }
    }

    private String serializeTechStack(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public boolean isStudent(String uuid) {
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return user.getRole() == Role.STUDENT;
    }

    @Transactional
    public void updateStatus(String uuid, boolean active, Authentication auth) {
        ensureAdminOrTeacher(auth);
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setActive(active);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(String uuid, Authentication auth) {
        ensureAdminOrTeacher(auth);
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        deleteUserOwnedData(user);
        userRepository.delete(user);
        userRepository.flush();
        evictUsernameCaches(user.getCustomId());
        var cache = cacheManager.getCache("users");
        if (cache != null) {
            cache.evict("profile:" + uuid);
            cache.evict(uuid);
        }
    }

    private void deleteUserOwnedData(User user) {
        String uuid = user.getUuid();
        Long userSeq = user.getSeq();

        executeDelete("""
                delete from comment_likes
                where user_seq = :userSeq
                   or comment_id in (
                       select id from comments
                       where user_id = :userSeq
                          or post_id in (select post_uuid from posts where writer_uuid = :uuid)
                   )
                """, uuid, userSeq);
        executeDelete("""
                update comments
                   set parent_id = null
                 where parent_id in (
                       select id from (
                           select id from comments
                           where user_id = :userSeq
                              or post_id in (select post_uuid from posts where writer_uuid = :uuid)
                       ) comments_to_delete
                 )
                """, uuid, userSeq);
        executeDelete("""
                delete from comments
                where user_id = :userSeq
                   or post_id in (select post_uuid from posts where writer_uuid = :uuid)
                """, uuid, userSeq);

        executeDelete("""
                delete from poll_votes
                where voter_uuid = :uuid
                   or poll_id in (
                       select id from polls
                       where post_uuid in (select post_uuid from posts where writer_uuid = :uuid)
                   )
                """, uuid, userSeq);
        executeDelete("""
                delete from poll_options
                where poll_id in (
                    select id from polls
                    where post_uuid in (select post_uuid from posts where writer_uuid = :uuid)
                )
                """, uuid, userSeq);
        executeDelete("""
                delete from polls
                where post_uuid in (select post_uuid from posts where writer_uuid = :uuid)
                """, uuid, userSeq);

        executeDelete("""
                delete from post_likes
                where user_uuid = :uuid
                   or post_uuid in (select post_uuid from posts where writer_uuid = :uuid)
                """, uuid, userSeq);
        executeDelete("""
                delete from post_bookmarks
                where user_uuid = :uuid
                   or post_uuid in (select post_uuid from posts where writer_uuid = :uuid)
                """, uuid, userSeq);
        executeDelete("""
                delete from notifications
                where recipient_uuid = :uuid
                   or sender_uuid = :uuid
                   or post_uuid in (select post_uuid from posts where writer_uuid = :uuid)
                """, uuid, userSeq);
        executeDelete("delete from posts where writer_uuid = :uuid", uuid, userSeq);

        executeDelete("""
                delete from study_group_join_request
                where user_uuid = :uuid
                   or group_uuid in (select group_uuid from study_group where owner_uuid = :uuid)
                """, uuid, userSeq);
        executeDelete("""
                delete from study_group_member
                where user_uuid = :uuid
                   or group_uuid in (select group_uuid from study_group where owner_uuid = :uuid)
                """, uuid, userSeq);
        executeDelete("delete from study_group where owner_uuid = :uuid", uuid, userSeq);

        executeDelete("delete from user_ban where user_seq = :userSeq or admin_seq = :userSeq", uuid, userSeq);
        executeDelete("delete from user_report where reporter_uuid = :userSeq or target_uuid = :userSeq", uuid, userSeq);
        executeDelete("delete from reports where reporter_uuid = :uuid or target_uuid = :uuid or resolved_by = :uuid", uuid, userSeq);
        executeDelete("delete from guestbook_entries where target_uuid = :uuid or writer_uuid = :uuid", uuid, userSeq);
        executeDelete("delete from cart_item where user_uuid = :uuid", uuid, userSeq);
        executeDelete("delete from trade_log where user_uuid = :uuid", uuid, userSeq);
        executeDelete("delete from user_inventory where user_uuid = :uuid", uuid, userSeq);
        executeDelete("delete from points_usage_history where user_uuid = :uuid", uuid, userSeq);
        executeDelete("delete from points_event_log where user_uuid = :uuid", uuid, userSeq);
        executeDelete("delete from user_points where user_uuid = :uuid", uuid, userSeq);
        executeDelete("delete from user_points_balance where user_uuid = :uuid", uuid, userSeq);
        executeDelete("delete from user_coins where user_uuid = :uuid", uuid, userSeq);
        executeDelete("delete from user_coins_balance where user_uuid = :uuid", uuid, userSeq);
        executeDelete("delete from user_projects where user_uuid = :uuid", uuid, userSeq);
        executeDelete("delete from user_data where uuid = :uuid", uuid, userSeq);
    }

    private int executeDelete(String sql, String uuid, Long userSeq) {
        var query = entityManager.createNativeQuery(sql);
        if (sql.contains(":uuid")) {
            query.setParameter("uuid", uuid);
        }
        if (sql.contains(":userSeq")) {
            query.setParameter("userSeq", userSeq);
        }
        return query.executeUpdate();
    }

    @Transactional
    public void resetPassword(String uuid, String newPassword, Authentication auth) {
        ensureAdminOrTeacher(auth);
        User user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public Map<String, Object> bulkCreateFromCsv(MultipartFile file, Authentication auth) {
        ensureAdminOrTeacher(auth);
        List<String> success = new ArrayList<>();
        List<Map<String, String>> failed = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] parts = line.split(",", -1);
                if (parts.length < 3) {
                    Map<String, String> err = new HashMap<>();
                    err.put("line", String.valueOf(lineNum));
                    err.put("reason", "최소 3개 컬럼(adminId,userRealname,userNickname) 필요");
                    failed.add(err);
                    continue;
                }
                String adminId = parts[0].trim();
                String userRealname = parts[1].trim();
                String userNickname = parts[2].trim();
                String customId = parts.length > 3 ? parts[3].trim() : null;
                String password = parts.length > 4 ? parts[4].trim() : null;

                if (adminId.isBlank() || userRealname.isBlank() || userNickname.isBlank()) {
                    Map<String, String> err = new HashMap<>();
                    err.put("line", String.valueOf(lineNum));
                    err.put("reason", "필수 값 누락");
                    failed.add(err);
                    continue;
                }
                try {
                    CreateUserRequest req = new CreateUserRequest();
                    req.setAdminId(adminId);
                    req.setUserRealname(userRealname);
                    req.setUserNickname(userNickname);
                    if (customId != null && !customId.isBlank()) req.setCustomId(customId);
                    if (password != null && !password.isBlank()) req.setPassword(password);
                    req.setRole(Role.STUDENT);
                    createUserByAdmin(req, auth);
                    success.add(adminId);
                } catch (Exception e) {
                    Map<String, String> err = new HashMap<>();
                    err.put("line", String.valueOf(lineNum));
                    err.put("adminId", adminId);
                    err.put("reason", e.getMessage());
                    failed.add(err);
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("CSV 파일을 읽을 수 없습니다: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", success.size());
        result.put("failedCount", failed.size());
        result.put("failed", failed);
        return result;
    }

    @Transactional(readOnly = true)
    public List<UserBalanceSummary> getAllStudentBalances(Authentication auth) {
        ensureAdminOrTeacher(auth);
        List<User> students = userRepository.findByRole(Role.STUDENT);
        List<UserBalanceSummary> result = new ArrayList<>();
        for (User u : students) {
            int coins = coinsBalanceRepository.findByUserUuid(u.getUuid())
                    .map(b -> b.getTotalCoins()).orElse(0);
            int points = pointsBalanceRepository.findByUserUuid(u.getUuid())
                    .map(b -> b.getTotalPoints()).orElse(0);
            result.add(UserBalanceSummary.builder()
                    .uuid(u.getUuid())
                    .adminId(u.getAdminId())
                    .customId(u.getCustomId())
                    .userRealname(u.getUserRealname())
                    .userNickname(u.getUserNickname())
                    .coinBalance(coins)
                    .pointBalance(points)
                    .build());
        }
        return result;
    }
}
