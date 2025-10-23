// src/main/java/com/ada/proj/entity/Post.java
package com.ada.proj.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "posts")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Post {
    // AI 일련번호(읽기 전용)
    @Column(name = "seq", insertable = false, updatable = false)
    private Long seq;

    // PK: 게시글 UUID
    @Id
    @Column(name = "post_uuid", length = 36, unique = true, nullable = false)
    private String postUuid;

    // FK: users.uuid (문자열로 보관)
    @Column(name = "writer_uuid", length = 36, nullable = false)
    private String writerUuid;

    @Column(name = "title", length = 20, nullable = false)
    private String title;

    @Lob
    @Column(name = "texts")
    private String texts;

    @Column(name = "images", length = 255)
    private String images;   // 이미지 URL

    @Column(name = "videos", length = 255)
    private String videos;   // 영상 URL

    @Column(name = "writer", length = 20)
    private String writer;   // 작성자 닉네임

    @Column(name = "writed_at")
    private LocalDateTime writedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "likes")
    private Integer likes;

    @Column(name = "views")
    private Integer views;

    @Column(name = "comments")
    private Integer comments;

    // 기본값 세팅
    @PrePersist
    public void onCreate() {
        if (this.postUuid == null) this.postUuid = UUID.randomUUID().toString();
        if (this.writedAt == null) this.writedAt = LocalDateTime.now();
        if (this.updatedAt == null) this.updatedAt = LocalDateTime.now();
        if (this.likes == null) this.likes = 0;
        if (this.views == null) this.views = 0;
        if (this.comments == null) this.comments = 0;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}