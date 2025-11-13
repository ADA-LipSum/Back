package com.ada.proj.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "comment_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_comment_user",
                columnNames = {"comment_id", "user_seq"}
        ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_seq", nullable = false)
    private User user;
}