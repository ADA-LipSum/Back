package com.ada.proj.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "post_bookmarks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_post_bookmark_user_post",
                        columnNames = {"user_uuid", "post_uuid"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_uuid", nullable = false, length = 36)
    private String userUuid;

    @Column(name = "post_uuid", nullable = false, length = 36)
    private String postUuid;
}
