package com.ada.proj.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @Column(nullable = false, unique = true, length = 36)
    private String uuid = UUID.randomUUID().toString();

    @Column(nullable = false, unique = true, length = 50)
    private String adminId;

    @Column(unique = true, length = 50)
    private String customId;

    @Column(length = 255)
    private String customPw;

    @Column(nullable = false, length = 10)
    private String userRealname;

    @Column(nullable = false, length = 10)
    private String userNickname;

    @Column(length = 255)
    private String profileImage;

    @Column(length = 255)
    private String profileBanner;

    @Column(nullable = false)
    private boolean isAnonymous = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
