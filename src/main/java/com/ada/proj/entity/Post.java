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

    // @Deprecated: 과거 이중 저장용 컬럼. 현재는 사용하지 않습니다(매핑 제거하여 혼란 방지).

    // Markdown/HTML 원문 단일 저장: contentMd만 사용 (contentHtml 더 이상 사용 안 함)
    @Lob
    @Column(name = "content_md", columnDefinition = "LONGTEXT")
    private String contentMd;

    // @Deprecated: 과거 렌더링된 HTML 저장 용도였으나 현재는 미사용. DB 컬럼은 남아있어도 매핑 제거.
    // (컬럼 유지 시 스키마 변경 없이 운용 가능)

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