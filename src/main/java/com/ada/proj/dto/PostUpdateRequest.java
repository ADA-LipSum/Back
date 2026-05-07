package com.ada.proj.dto;

import java.util.List;

import com.ada.proj.enums.CommunityCategory;
import com.ada.proj.enums.PostBoardType;
import com.ada.proj.enums.TechSubTag;
import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostUpdateRequest {

    @Size(max = 20)
    private String title;

    @JsonAlias({"contentMd"})
    private String content;

    private String images;
    private String videos;

    private Boolean isDev;
    private String devTags;

    private PostBoardType boardType;
    private CommunityCategory communityCategory;
    private TechSubTag techSubTag;
    private List<String> techTags;
    private String thumbnailImage;
    private PollCreateRequest poll;
}
