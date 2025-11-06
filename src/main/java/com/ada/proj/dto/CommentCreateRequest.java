package com.ada.proj.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentCreateRequest {
    private String postId;
    private Long parentId; // nullable
    private String content;
}
