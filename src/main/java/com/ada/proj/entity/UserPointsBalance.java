package com.ada.proj.entity;

import java.time.Instant;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_points_balance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPointsBalance {

    @Id
    @Column(name = "user_uuid", length = 36, nullable = false)
    private String userUuid;

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
