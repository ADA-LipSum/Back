package com.ada.proj.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "points_usage_history",
        indexes = {
                @Index(name = "idx_points_usage_user_uuid", columnList = "user_uuid"),
                @Index(name = "idx_points_usage_points_uuid", columnList = "points_uuid")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointsUsageHistory {

    @Id
    @Column(name = "usage_uuid", length = 36)
    private String usageUuid; // assigned

    @Column(name = "user_uuid", length = 36, nullable = false)
    private String userUuid;

    @Column(name = "points_uuid", length = 36, nullable = false)
    private String pointsUuid;

    @Column(name = "used_for", length = 100, nullable = false)
    private String usedFor;

    @Column(name = "metadata", columnDefinition = "json")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
