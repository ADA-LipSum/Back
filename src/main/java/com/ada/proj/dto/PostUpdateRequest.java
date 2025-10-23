// src/main/java/com/ada/proj/dto/post/PostUpdateRequest.java
package com.ada.proj.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PostUpdateRequest {
    @Size(max = 20)
    private String title;
    private String texts;
    private String images;
    private String videos;
    private String writer;
}