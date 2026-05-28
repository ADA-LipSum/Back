package com.ada.proj.entity;

import java.time.Instant;

import com.ada.proj.enums.CustomStickerStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "custom_sticker",
        indexes = {
                @Index(name = "idx_custom_sticker_uuid", columnList = "sticker_uuid"),
                @Index(name = "idx_custom_sticker_user_uuid", columnList = "user_uuid"),
                @Index(name = "idx_custom_sticker_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_custom_sticker_uuid", columnNames = {"sticker_uuid"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomSticker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sticker_uuid", length = 36, nullable = false, unique = true)
    private String stickerUuid;

    @Column(name = "user_uuid", length = 36, nullable = false)
    private String userUuid;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "image_url", length = 500, nullable = false)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private CustomStickerStatus status;

    @Column(name = "submission_fee", nullable = false)
    private Integer submissionFee;

    @Column(name = "points_uuid", length = 36)
    private String pointsUuid;

    @Column(name = "trade_item_uuid", length = 36)
    private String tradeItemUuid;

    @Column(name = "reviewed_by", length = 36)
    private String reviewedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
