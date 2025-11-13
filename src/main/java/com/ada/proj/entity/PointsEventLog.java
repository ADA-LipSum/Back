package com.ada.proj.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "points_event_log",
        indexes = {
                @Index(name = "idx_points_event_user_uuid", columnList = "user_uuid"),
                @Index(name = "idx_points_event_rule_id", columnList = "rule_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointsEventLog {

    @Id
    @Column(name = "event_uuid", length = 36)
    private String eventUuid; // assigned

    @Column(name = "user_uuid", length = 36, nullable = false)
    private String userUuid;

    @Column(name = "rule_id")
    private Long ruleId;

    @Column(name = "points_uuid", length = 36)
    private String pointsUuid;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
