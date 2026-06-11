package com.ada.proj.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_projects")
public class UserProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 36, nullable = false)
    private String userUuid; // FK -> users.uuid

    @Column(length = 100, nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "github_url", length = 500)
    private String githubUrl;

    @Column(name = "is_looking_for_team", nullable = false)
    @Builder.Default
    private boolean isLookingForTeam = false;

    // 관리자가 우수 프로젝트(순위)로 선정했는지 여부 - true로 처음 바뀔 때 작성자에게 코인 지급
    @Column(name = "selected", nullable = false)
    @Builder.Default
    private boolean selected = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
