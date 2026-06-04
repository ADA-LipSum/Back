package com.ada.proj.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "post_emoji_reactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_post_emoji_user",
                        columnNames = {"post_uuid", "user_uuid", "emoji"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostEmojiReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_uuid", length = 36, nullable = false)
    private String postUuid;

    @Column(name = "user_uuid", length = 36, nullable = false)
    private String userUuid;

    @Column(name = "emoji", length = 20, nullable = false)
    private String emoji;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }
}
