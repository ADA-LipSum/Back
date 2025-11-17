package com.ada.proj.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "post_likes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_post_like_user_post",
                        columnNames = {"user_uuid", "post_uuid"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_uuid", nullable = false, length = 36)
    private String userUuid;

    @Column(name = "post_uuid", nullable = false, length = 36)
    private String postUuid;
}