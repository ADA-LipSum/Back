package com.ada.proj.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ada.proj.enums.NoticeTag;

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
@Table(name = "notices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice {

    @Id
    @Column(name = "notice_uuid", length = 36, nullable = false)
    private String noticeUuid;

    @Column(name = "seq", updatable = false)
    private Long seq;

    @Column(name = "writer_uuid", length = 36, nullable = false)
    private String writerUuid;

    @Column(name = "writer", length = 20)
    private String writer;

    @Column(name = "title", length = 100, nullable = false)
    private String title;

    @Lob
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    // 쉼표(,) 구분 첨부파일 URL 목록
    @Lob
    @Column(name = "attachment_urls", columnDefinition = "LONGTEXT")
    private String attachmentUrls;

    @Enumerated(EnumType.STRING)
    @Column(name = "tag", length = 20)
    private NoticeTag tag;

    @Column(name = "is_pinned")
    private Boolean isPinned;

    @Column(name = "pinned_order")
    private Integer pinnedOrder;

    @Column(name = "pinned_at")
    private LocalDateTime pinnedAt;

    @Column(name = "views")
    private Integer views;

    @Column(name = "writed_at")
    private LocalDateTime writedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        if (this.noticeUuid == null) this.noticeUuid = UUID.randomUUID().toString();
        if (this.writedAt == null) this.writedAt = LocalDateTime.now();
        if (this.updatedAt == null) this.updatedAt = LocalDateTime.now();
        if (this.views == null) this.views = 0;
        if (this.isPinned == null) this.isPinned = false;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
