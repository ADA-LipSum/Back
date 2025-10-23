package com.ada.proj.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_uuid", columnNames = {"uuid"}),
                @UniqueConstraint(name = "uk_users_admin_id", columnNames = {"admin_id"}),
                @UniqueConstraint(name = "uk_users_custom_id", columnNames = {"custom_id"})
        })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @Column(length = 36, nullable = false)
    private String uuid;

    @Column(name = "admin_id", length = 50, nullable = false)
    private String adminId;

    @Column(name = "custom_id", length = 50)
    private String customId;

    @Column(name = "custom_pw", length = 255)
    private String customPw; // BCrypt 해시 저장

    @Column(name = "user_realname", length = 10, nullable = false)
    private String userRealname;

    @Column(name = "user_nickname", length = 10, nullable = false)
    private String userNickname;

    @Column(name = "profile_image", length = 255)
    private String profileImage;

    @Column(name = "profile_banner", length = 255)
    private String profileBanner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // 닉네임을 이름으로 표시 여부 (요구 이미지의 토글 기능)
        @Column(name = "use_nickname", nullable = false)
        @Builder.Default
        private boolean useNickname = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
