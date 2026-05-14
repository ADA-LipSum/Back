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
public class PostDetailResponse {

    private String postUuid;
    private Long seq;

    private String writerUuid;
    private String writerCustomId;
    private String writer;
    private String writerProfileImage;

    private String title;
    private String content;
    private String images;
    private String videos;
    private String thumbnailImage;

    private LocalDateTime writedAt;
    private LocalDateTime updatedAt;

    private Integer likes;
    private Integer views;
    private Integer comments;

    private Boolean isDev;
    private String devTags;
    private List<String> techTags;

    private PostBoardType boardType;
    private CommunityCategory communityCategory;
    private TechSubTag techSubTag;
    private PollResponse poll;

    private Boolean isLiked;
    private Boolean isBookmarked;
}
