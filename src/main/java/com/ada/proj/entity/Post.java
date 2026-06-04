package com.ada.proj.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ada.proj.enums.CommunityCategory;
import com.ada.proj.enums.NoticeCategory;
import com.ada.proj.enums.PostBoardType;
import com.ada.proj.enums.TechSubTag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Column(name = "seq", updatable = false)
    private Long seq;

    @Id
    @Column(name = "post_uuid", length = 36, unique = true, nullable = false)
    private String postUuid;

    @Column(name = "writer_uuid", length = 36, nullable = false)
    private String writerUuid;

    @Column(name = "title", length = 20, nullable = false)
    private String title;

    @Lob
    @Column(name = "content_md", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "images", length = 255)
    private String images;

    @Column(name = "videos", length = 255)
    private String videos;

    @Column(name = "writer", length = 20)
    private String writer;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "board_type", length = 20)
    private PostBoardType boardType;

    @Enumerated(EnumType.STRING)
    @Column(name = "community_category", length = 30)
    private CommunityCategory communityCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "tech_sub_tag", length = 20)
    private TechSubTag techSubTag;

    @Column(name = "thumbnail_image", length = 455)
    private String thumbnailImage;

    @Column(name = "is_dev")
    private Boolean isDev;

    @Column(name = "dev_tags", length = 255)
    private String devTags;

    @Enumerated(EnumType.STRING)
    @Column(name = "notice_category", length = 20)
    private NoticeCategory noticeCategory;

    @Column(name = "is_pinned")
    private Boolean isPinned;

    @Column(name = "pinned_at")
    private LocalDateTime pinnedAt;

    @PrePersist
    public void onCreate() {
        if (this.postUuid == null) this.postUuid = UUID.randomUUID().toString();
        if (this.writedAt == null) this.writedAt = LocalDateTime.now();
        if (this.updatedAt == null) this.updatedAt = LocalDateTime.now();
        if (this.likes == null) this.likes = 0;
        if (this.views == null) this.views = 0;
        if (this.comments == null) this.comments = 0;
        if (this.isDev == null) this.isDev = false;
        if (this.boardType == null) this.boardType = PostBoardType.COMMUNITY;
        if (this.isPinned == null) this.isPinned = false;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
