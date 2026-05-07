package com.ada.proj.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.ada.proj.enums.CommunityCategory;
import com.ada.proj.enums.PostBoardType;
import com.ada.proj.enums.TechSubTag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostSummaryResponse {

    private String postUuid;
    private Long seq;
    private String title;

    private String writer;
    private String writerProfileImage;

    private LocalDateTime writedAt;
    private Integer likes;
    private Integer views;
    private Integer comments;

    private Boolean isDev;
    private String devTags;
    private String tag;
    private List<String> techTags;

    private PostBoardType boardType;
    private CommunityCategory communityCategory;
    private TechSubTag techSubTag;
    private String thumbnailImage;

    public PostSummaryResponse(
            String postUuid,
            Long seq,
            String title,
            String writer,
            String writerProfileImage,
            LocalDateTime writedAt,
            Integer likes,
            Integer views,
            Integer comments,
            Boolean isDev,
            String devTags,
            String tag,
            PostBoardType boardType,
            CommunityCategory communityCategory,
            TechSubTag techSubTag,
            String thumbnailImage
    ) {
        this.postUuid = postUuid;
        this.seq = seq;
        this.title = title;
        this.writer = writer;
        this.writerProfileImage = writerProfileImage;
        this.writedAt = writedAt;
        this.likes = likes;
        this.views = views;
        this.comments = comments;
        this.isDev = isDev;
        this.devTags = devTags;
        this.tag = tag;
        this.boardType = boardType;
        this.communityCategory = communityCategory;
        this.techSubTag = techSubTag;
        this.thumbnailImage = thumbnailImage;
    }
}
