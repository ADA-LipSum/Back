package com.ada.proj.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.ada.proj.enums.GroupStatus;
import com.ada.proj.enums.GroupVisibility;
import com.ada.proj.enums.StudyGroupCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "study_group",
        indexes = {
            @Index(name = "idx_study_group_uuid", columnList = "group_uuid", unique = true),
            @Index(name = "idx_study_group_status", columnList = "status"),
            @Index(name = "idx_study_group_visibility", columnList = "visibility"),
            @Index(name = "idx_study_group_owner", columnList = "owner_uuid")
        })
public class StudyGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_uuid", length = 36, nullable = false, unique = true)
    private String groupUuid;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "tech_tags", length = 1000)
    private String techTags; // comma-separated

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", length = 20, nullable = false)
    private GroupVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private GroupStatus status;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "owner_uuid", length = 36, nullable = false)
    private String ownerUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20, nullable = false)
    private StudyGroupCategory category;

    @Column(name = "thumbnail_image", length = 455)
    private String thumbnailImage;

    @Column(name = "invite_link", length = 500)
    private String inviteLink; // 승인된 멤버에게만 노출되는 초대 코드/링크(디스코드, 카카오톡 오픈채팅 등)

    @Column(name = "activity_start_date")
    private LocalDate activityStartDate;

    @Column(name = "activity_end_date")
    private LocalDate activityEndDate;

    @Column(name = "activity_type", length = 200)
    private String activityType; // 활동 방식(예: 온/오프라인, 모임 주기 등 자유 텍스트)

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (groupUuid == null) {
            groupUuid = UUID.randomUUID().toString();
        }
        if (visibility == null) {
            visibility = GroupVisibility.PUBLIC;
        }
        if (status == null) {
            status = GroupStatus.OPEN;
        }
    }
}
