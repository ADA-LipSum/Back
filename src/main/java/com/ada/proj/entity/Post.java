// src/main/java/com/ada/proj/entity/Post.java
package com.ada.proj.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "posts")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Post {
    // 자동 일련번호
    @Column(name = "seq", insertable = false, updatable = false)
    private Long seq;

    // PK
    @Id
    @Column(name = "post_uuid", length = 36, unique = true, nullable = false)
    private String postUuid;

    // 작성자 UUID
    @Column(name = "writer_uuid", length = 36, nullable = false)
    private String writerUuid;

    @Column(name = "title", length = 20, nullable = false)
    private String title;

    // 원문(마크다운/HTML)
    @Lob
    @Column(name = "content_md", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "images", length = 255)
    private String images;   // 이미지 URL

    @Column(name = "videos", length = 255)
    private String videos;   // 영상 URL

    @Column(name = "writer", length = 20)
    private String writer;   // 작성자

    @Column(name = "writed_at")
    private LocalDateTime writedAt;   // 작성일

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;  // 수정일

    @Column(name = "likes")
    private Integer likes;   // 좋아요

    @Column(name = "views")
    private Integer views;   // 조회수

    @Column(name = "comments")
    private Integer comments; // 댓글 수

    // 개발글 여부/언어
    @Column(name = "is_dev")
    private Boolean isDev;   // 개발글 여부

    @Column(name = "dev_tags", length = 255)
    private String devTags;  // 언어 CSV (예: Python,C)

    // 기본값
    @PrePersist
    public void onCreate() {
        if (this.postUuid == null) this.postUuid = UUID.randomUUID().toString();
        if (this.writedAt == null) this.writedAt = LocalDateTime.now();
        if (this.updatedAt == null) this.updatedAt = LocalDateTime.now();
        if (this.likes == null) this.likes = 0;
        if (this.views == null) this.views = 0;
        if (this.comments == null) this.comments = 0;
        if (this.isDev == null) this.isDev = false;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}